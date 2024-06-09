package com.github.foldcat.dislocated.impl.client.actor

import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.util.valve.*
import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import fabric.*
import fabric.filter.*
import fabric.io.*
import org.apache.pekko
import org.apache.pekko.http.scaladsl.model.*
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.*
import pekko.http.scaladsl.unmarshalling.*
import pekko.stream.*
import pekko.stream.scaladsl.*
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

enum ApiCall:
  case Call(
      effect: HttpRequest,
      promise: Promise[Json]
  )
  case OpenValve

class HttpActor(
    context: ActorContext[ApiCall],
    timer: TimerScheduler[ApiCall],
    callInCapacity: Int
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  // when receive message, push here
  val (
    (callIn, callInValveFuture: Future[ValveSwitch]),
    callInProbe
  ) = Source
    .queue[Call](callInCapacity)
    .viaMat(new Valve(SwitchMode.Open))(Keep.both)
    .toMat(Sink.foreach(handleCallIn))(Keep.both)
    .run()

  val callInValve =
    var tempStore: Option[ValveSwitch] = None
    val trg = callInValveFuture.map(swt => tempStore = Some(swt))
    Await.ready(trg, 1.second)
    tempStore match
      case None =>
        logger.error("cannot find")
        throw new WebsocketFailure("failure to get valve switch")
      case Some(value) =>
        value

  extension [T](o: Option[T])
    def unwrap =
      o match
        case None =>
          throw WebsocketFailure("unwrap failure")
        case Some(value) =>
          value

  extension [T](f: Future[T])
    def goBlock(s: FiniteDuration) =
      Await.ready(f, s)

  extension (s: HttpResponse)
    def getHeaderValue(find: String) =
      val range = s.headers
      range
        .find(h =>
          // logger.info(s"${h.name} $find ${h.name == find}")
          h.name == find
        )
        .flatMap(r => Option(r.value))

  def handleCallIn(call: Call) =
    executeRequest(call)

  def executeRequest(req: ApiCall.Call) =
    val (effect, promise) = req match
      case Call(effect, promise) =>
        (effect, promise)

    val futureRequest = Http()
      .singleRequest(effect)
      .map(resp =>
        logger.info("got response")

        val target = Unmarshal(resp)
          .to[String]
          .map(out =>
            SnakeToCamelFilter(
              JsonParser(out, Format.Json),
              JsonPath.empty
            ) match
              case None =>
                throw new WebsocketFailure(
                  "fail to parse json in snake case"
                )
              case Some(value) =>
                logger.info(value.toString)
                promise.success(value)

            resp.getHeaderValue("x-ratelimit-remaining") match
              case None =>
              case Some("0") =>
                logger.warn("no rate limit left")
                timer.startSingleTimer(
                  OpenValve,
                  resp
                    .getHeaderValue("x-ratelimit-reset-after")
                    .unwrap
                    .toDouble
                    .second
                )
                callInValve
                  .flip(SwitchMode.Close)
                  .goBlock(1.second)

              case Some(value) =>
                logger.info(s"remaining $value")
          )
        Await.ready(target, 5.second)
      )
    Await.ready(futureRequest, 5.second)

  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        callIn !< call
      case OpenValve =>
        context.log.info("rate limit awaited")
        callInValve
          .flip(SwitchMode.Open)
          .goBlock(1.second)
    this

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.info("restarting http funnel")
      this

end HttpActor

object HttpActor:
  def apply(): Behavior[ApiCall] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timer =>
        new HttpActor(context, timer, 100)
      )
    )
