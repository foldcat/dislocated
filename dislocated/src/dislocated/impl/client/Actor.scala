package com.github.foldcat.dislocated.impl.client.actor

import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import fabric.*
import fabric.filter.*
import fabric.io.*
import java.util.concurrent.atomic.*
import org.apache.pekko
import org.apache.pekko.http.scaladsl.model.*
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.*
import pekko.http.scaladsl.unmarshalling.*
import scala.collection.mutable.*
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

enum ApiCall:
  case Call(
      effect: HttpRequest,
      promise: Promise[Json]
  )
  case QueueCall

class HttpActor(
    context: ActorContext[ApiCall],
    timer: TimerScheduler[ApiCall]
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  // true: can occupy
  // false: in use
  val semaphore = AtomicBoolean(true)

  // use queue call when init
  var initialized = false

  val stash: Queue[Call] = Queue.empty

  extension [T](o: Option[T])
    def unwrap =
      o match
        case None =>
          throw WebsocketFailure("unwrap failure")
        case Some(value) =>
          value

  extension (s: HttpResponse)
    def getHeaderValue(find: String) =
      val range = s.headers
      range
        .find(h => h.name == find)
        .flatMap(r => Option(r.value))

  def executeRequest(req: ApiCall.Call) =
    val (effect, promise) = req match
      case Call(effect, promise) =>
        (effect, promise)

    // aquire
    semaphore.set(false)

    Http()
      .singleRequest(effect)
      .flatMap(resp =>
        logger.info("got response")
        Unmarshal(resp)
          .to[String]
          .map(out => (out, resp))
      )
      .map((out, resp) =>
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
              QueueCall,
              resp
                .getHeaderValue("x-ratelimit-reset-after")
                .unwrap
                .toDouble
                .second
            )
          case Some(value) =>
            logger.info(s"remaining $value")
            context.self ! QueueCall
      )
      .onComplete(x => logger.info("req done"))

  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        context.log.info("got call")
        if !initialized then
          initialized = true
          context.log.info("first call")
          executeRequest(call)
        else if initialized && semaphore.get then
          context.log.info("can call")
          executeRequest(call)
        else
          context.log.info("stashing")
          stash.enqueue(call)

      // dequeue and call if possible
      case QueueCall =>
        context.log.info("got queuecall")
        if !stash.isEmpty then
          context.log.info("dequeue and run")
          executeRequest(stash.dequeue)
        else if stash.isEmpty then
          context.log.info("end of chain")
          semaphore.set(true)
        else
          context.log.info("undefined behavior")
          throw WebsocketFailure("queue call undefined behavior")

    this

  end onMessage

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.info("restarting http funnel")
      this

end HttpActor

object HttpActor:
  def apply(): Behavior[ApiCall] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timer => new HttpActor(context, timer))
    )
