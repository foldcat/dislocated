package com.github.foldcat.dislocated.impl.client.actor

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

class OneOffExecutor(context: ActorContext[Nothing], f: () => Any)
    extends AbstractBehavior[Nothing](context):
  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
  context.log.info("one off execution firing")
  f()
  Behaviors.stopped

object OneOffExecutor:
  /** async execute f
    *
    * @param f
    *   lambda to be executed
    * @return
    *   nothing
    */
  def apply(f: () => Any): Behavior[Nothing] =
    Behaviors.setup(context => new OneOffExecutor(context, f))

enum ApiCall:
  case Call(
      effect: HttpRequest,
      promise: Promise[Json]
  )

class HttpActor(
    context: ActorContext[ApiCall],
    timer: TimerScheduler[ApiCall],
    callInCapacity: Int,
    stashCapacity: Int
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  // calls left
  var callLeft: Option[Int] = None

  val ((stash, stashValveFuture), stashProbe) = Source
    .queue[ApiCall](stashCapacity)
    .viaMat(new Valve(SwitchMode.Close))(Keep.both)
    .toMat(Sink.foreach(handleStash))(Keep.both)
    .run()

  // flow or not
  val stashValve =
    var tempStore: Option[ValveSwitch] = None
    val trg = stashValveFuture.map(swt => tempStore = Some(swt))
    Await.ready(trg, 1.second)
    tempStore match
      case None =>
        throw new IllegalStateException("failure to get valve switch")
      case Some(value) =>
        value

  // when receive message, push here
  val ((callIn, callInValveFuture), callInProbe) = Source
    .queue[ApiCall](callInCapacity)
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
        throw new IllegalStateException("failure to get valve switch")
      case Some(value) =>
        value

  def handleCallIn(call: ApiCall) =
    callLeft match
      case None =>
        executeRequest(call)
      case Some(remaining) =>
        if remaining > 0 && stash.size() == 0 then
          logger.info("can call, running now")
          executeRequest(call)
        else if remaining > 0 && stash.size() > 0 then
          logger.info("can call, emptying stash")
          stash !< call
          stashValve.flip(SwitchMode.Open).map(identity)
        else if remaining <= 0 then
          logger.info("cannot call, scheduling refill")
          logger.info("stashing call")
          // timer.startSingleTimer()
          stash !< call

  def handleStash(call: ApiCall) =
    callLeft match
      case None =>
        throw new IllegalStateException("callleft is none")
      case Some(remaining) =>

  extension (s: HttpResponse)
    def getHeaderValue(find: String) =
      val range = s.headers
      range.find(h => h.name == find) match
        case None =>
          throw new IllegalStateException("header not found")
        case Some(value) =>
          value.value

  def executeRequest(req: ApiCall) =
    val (effect, promise) = req match
      case Call(effect, promise) =>
        (effect, promise)

    val futureRequest = Http()
      .singleRequest(effect)
      .map(resp =>
        logger.info("got response")
        resp.getHeaders.forEach(println)
        val target = Unmarshal(resp)
          .to[String]
          .map(out =>
            SnakeToCamelFilter(
              JsonParser(out, Format.Json),
              JsonPath.empty
            ) match
              case None =>
                throw new IllegalStateException(
                  "fail to parse json in snake case"
                )
              case Some(value) =>
                logger.info(value.toString)
                promise.success(value)
          )
        Await.ready(target, 5.second)
      )
    Await.ready(futureRequest, 5.second)
  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    callIn !< msg
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
        new HttpActor(context, timer, 100, 100)
      )
    )
