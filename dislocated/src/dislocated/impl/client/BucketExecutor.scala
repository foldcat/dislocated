package com.github.foldcat.dislocated.impl.client.bucketexecutor

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.registry.Registry
import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.util.label.Label.genLabel
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
import scala.collection.mutable.*
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

// TODO: self terminate
class HttpActor(
    context: ActorContext[ApiCall],
    timer: TimerScheduler[ApiCall],
    reg: Registry
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  // true: can occupy
  // false: in use
  var semaphore = true

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
    val (effect, promise, uri) = req match
      case Call(effect, promise, uri) =>
        (effect, promise, uri)

    // aquire
    semaphore = false

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

        val bucket = resp.getHeaderValue("x-ratelimit-bucket").unwrap
        resp.getHeaderValue("x-ratelimit-remaining") match
          case None =>
          case Some("0") =>
            logger.warn("no rate limit left")
            timer.startSingleTimer(
              QueueCall(uri, bucket),
              resp
                .getHeaderValue("x-ratelimit-reset-after")
                .unwrap
                .toDouble
                .second
            )
          case Some(value) =>
            logger.info(s"remaining $value")
            context.self ! QueueCall(uri, bucket)
      )
      .onComplete(x => logger.info("req done"))

  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        context.log.info("got call")
        if semaphore then
          context.log.info("can call")
          executeRequest(call)
        else
          context.log.info("stashing")
          stash.enqueue(call)
        this

      // dequeue and call if possible
      case QueueCall(uri, bucket) =>
        context.log.info("got queuecall")

        if reg.update(uri, bucket) then
          context.log.info(s"making new actor $bucket")
          reg.registerActor(
            bucket,
            context.spawn(
              HttpActor(reg),
              genLabel("http-bucket-executor-" + bucket)
            )
          )

        if !stash.isEmpty then
          context.log.info("dequeue and run")
          executeRequest(stash.dequeue)
        else if stash.isEmpty then
          context.log.info("end of chain")
          semaphore = true

    end match

    this

  end onMessage

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.info("restarting http funnel")
      this

end HttpActor

object HttpActor:
  def apply(reg: Registry): Behavior[ApiCall] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timer =>
        new HttpActor(context, timer, reg)
      )
    )
