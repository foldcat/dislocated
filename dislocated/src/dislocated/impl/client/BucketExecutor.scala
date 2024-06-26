package com.github.foldcat.dislocated.impl.client.bucketexecutor

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.apicall.QueuedExecution
import com.github.foldcat.dislocated.impl.client.registry.Registry
import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.util.label.Label.genLabel
import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import fabric.*
import fabric.filter.*
import fabric.io.*
import java.time.LocalDateTime
import java.util.concurrent.*
import org.apache.pekko
import org.apache.pekko.http.scaladsl.model.*
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.*
import pekko.http.scaladsl.unmarshalling.*
import pekko.stream.*
import scala.collection.mutable.*
import scala.concurrent.*
import scala.concurrent.duration.*

class HttpActor(
    context: ActorContext[ApiCall],
    timer: TimerScheduler[ApiCall],
    registry: Registry,
    // is said actor default?
    isEntry: Boolean,
    bucketId: Option[String],
    initUri: Option[String],
    executor: BoundedSourceQueue[Defer[Any]]
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*
  import QueuedExecution.*

  implicit val system: ActorSystem[Nothing] = context.system

  implicit val ec: ExecutionContext = system.executionContext

  // 30 second inactive killswitch
  if !isEntry then context.setReceiveTimeout(10.second, Terminate)

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  val knownUri: Set[String] = Set.empty

  logger.trace(s"executor: $executor")

  initUri match
    case None =>
    case Some(value) =>
      knownUri += value

  // true: can occupy
  // false: in use
  val semaphore = atomic.AtomicBoolean(true)

  val stash: PriorityQueue[QueuedExecution] = PriorityQueue.empty

  context.log.trace("new http bucket spawned")
  context.log.trace(s"default status: $isEntry")

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

  def rightNow = LocalDateTime.now().getNano()

  def executeRequest(req: ApiCall.Call) =
    val (effect, promise, uri) = req match
      case Call(effect, promise, uri) =>
        (effect, promise, uri)

    // aquire
    semaphore.set(false)

    // TODO: given error
    executor !< Defer(effect =
      () =>
        Http()
          .singleRequest(effect)
          .flatMap(resp =>
            logger.trace("got response")
            Unmarshal(resp)
              .to[String]
              .map(out => (out, resp))
          )
          .map((out, resp) =>
            try
              val bucket = resp.getHeaderValue("x-ratelimit-bucket")

              context.self ! SetInfo(bucket, uri)

              val status = resp.status.intValue

              logger.trace(s"bucket: $bucket")

              val data =
                SnakeToCamelFilter(
                  JsonParser(out, Format.Json),
                  JsonPath.empty
                ) match
                  case None =>
                    throw new WebsocketFailure(
                      "fail to parse json in snake case"
                    )
                  case Some(value) =>
                    logger.trace(value.toString)
                    value

              if status == 200 then
                promise.success(data)
                resp.getHeaderValue("x-ratelimit-remaining") match
                  case None =>
                  case Some("0") =>
                    logger.warn("no rate limit left")
                    timer.startSingleTimer(
                      QueueCall(uri, bucket.unwrap),
                      resp
                        .getHeaderValue("x-ratelimit-reset-after")
                        .unwrap
                        .toDouble
                        .second
                    )
                  case Some(value) =>
                    logger.trace(s"remaining $value")
                    context.self ! QueueCall(uri, bucket.unwrap)
              else if status == 429 then
                logger.warn("429 too many requests")
                context.self ! Prior(
                  QueuedExecution(0, req, rightNow)
                )
                timer.startSingleTimer(
                  QueueCall(uri, bucket.unwrap),
                  data("retryAfter").asDouble.seconds
                )
            finally 
              resp.discardEntityBytes()
          )
    )

  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        context.log.trace("got call")
        if semaphore.get then
          context.log.trace("can call")
          executeRequest(call)
        else
          context.log.trace("stashing")
          stash.enqueue(QueuedExecution(1, call, rightNow))
        this

      // dequeue and call if possible
      case QueueCall(uri, bucket) =>
        context.log.trace("got queuecall")

        if registry.update(uri, bucket) then
          context.log.trace(s"making new actor $bucket")
          registry.registerActor(
            bucket,
            context.spawn(
              HttpActor(
                registry = registry,
                isEntry = false,
                bucketId = Some(bucket),
                initUri = Some(uri),
                executor = executor
              ),
              genLabel("http-bucket-executor-" + bucket)
            )
          )

        if !stash.isEmpty then
          context.log.trace("dequeue and run")
          executeRequest(stash.dequeue.call)
          // if not empty, cancel timeout
          if !isEntry then context.cancelReceiveTimeout()
        else if stash.isEmpty then
          context.log.trace("end of chain")
          semaphore.set(true)
          // if is empty, time self out
          if !isEntry then
            context.log.trace("timing out in 10s")
            context.setReceiveTimeout(10.second, Terminate)
        this

      case Prior(call) =>
        stash.enqueue(call)
        this

      case SetInfo(bucket, uri) =>
        context.log.trace("set info called")
        context.log.trace(isEntry.toString)
        if !isEntry then
          logger.trace(s"setting $bucket")
          knownUri += uri
        this

      case Terminate =>
        Behaviors.stopped

    end match

  end onMessage

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.trace("restarting http funnel")
      this
    case PostStop =>
      if !isEntry then
        context.log.trace("poststop cleanup")

        context.log.trace(bucketId.toString)
        context.log.trace(knownUri.mkString(","))

        registry.bucketStore.remove(bucketId.unwrap)
        knownUri.foreach(s => registry.uriStore.remove(s))
        context.log.trace("done")
        context.log.trace(registry.bucketStore.toString)
        context.log.trace(registry.uriStore.toString)
      this

end HttpActor

object HttpActor:
  def apply(
      registry: Registry,
      isEntry: Boolean,
      bucketId: Option[String],
      initUri: Option[String],
      executor: BoundedSourceQueue[Defer[Any]]
  ): Behavior[ApiCall] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timer =>
        new HttpActor(
          context,
          timer,
          registry,
          isEntry,
          bucketId,
          initUri,
          executor
        )
      )
    )
