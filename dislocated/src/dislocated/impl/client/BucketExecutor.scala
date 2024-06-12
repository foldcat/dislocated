package com.github.foldcat.dislocated.impl.client.bucketexecutor

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.registry.Registry
import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.util.label.Label.genLabel
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
    reg: Registry,
    // is said actor default?
    isEntry: Boolean,
    bucketId: Option[String]
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  // 30 second inactive killswitch
  if !isEntry then context.setReceiveTimeout(30.second, Terminate)

  val.traceger = LoggerFactory.getLogger(classOf[HttpActor])

  val knownUri: Set[String] = Set.empty

  // true: can occupy
  // false: in use
  var semaphore = true

  val stash: Queue[Call] = Queue.empty

  context.trace.info("new http bucket spawned")
  context.trace.info(s"default status: $isEntry")

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
       .traceger.info("got response")
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
           .traceger.info(value.toString)
            promise.success(value)

        val bucket = resp.getHeaderValue("x-ratelimit-bucket")
       .traceger.info(s"bucket: $bucket")

        context.self ! SetInfo(bucket, uri)

        resp.getHeaderValue("x-ratelimit-remaining") match
          case None =>
          case Some("0") =>
           .traceger.warn("no rate limit left")
            timer.startSingleTimer(
              QueueCall(uri, bucket.unwrap),
              resp
                .getHeaderValue("x-ratelimit-reset-after")
                .unwrap
                .toDouble
                .second
            )
          case Some(value) =>
           .traceger.info(s"remaining $value")
            context.self ! QueueCall(uri, bucket.unwrap)
      )
      .onComplete(x =>.traceger.info("req done"))

  end executeRequest

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        context.trace.info("got call")
        if semaphore then
          context.trace.info("can call")
          executeRequest(call)
        else
          context.trace.info("stashing")
          stash.enqueue(call)
        this

      // dequeue and call if possible
      case QueueCall(uri, bucket) =>
        context.trace.info("got queuecall")

        if reg.update(uri, bucket) then
          context.trace.info(s"making new actor $bucket")
          reg.registerActor(
            bucket,
            context.spawn(
              HttpActor(reg, false, Some(bucket)),
              genLabel("http-bucket-executor-" + bucket)
            )
          )

        if !stash.isEmpty then
          context.trace.info("dequeue and run")
          executeRequest(stash.dequeue)
          // if not empty, cancel timeout
          if !isEntry then context.cancelReceiveTimeout()
        else if stash.isEmpty then
          context.trace.info("end of chain")
          semaphore = true
          // if is empty, time self out
          if !isEntry then
            context.trace.info("timing out in 30s")
            context.setReceiveTimeout(30.second, Terminate)
        this

      case SetInfo(bucket, uri) =>
        context.trace.info("set info called")
        context.trace.info(isEntry.toString)
        if !isEntry then
         .traceger.info(s"setting $bucket")
          knownUri += uri
        this

      case Terminate =>
        Behaviors.stopped

    end match

  end onMessage

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.trace.info("restarting http funnel")
      this
    case PostStop =>
      if !isEntry then
        context.trace.info("poststop cleanup")

        context.trace.trace(bucketId.toString)
        context.trace.trace(knownUri.mkString(","))

        reg.bucketStore.remove(bucketId.unwrap)
        knownUri.foreach(s => reg.uriStore.remove(s))
        context.trace.info("done")
        context.trace.trace(reg.bucketStore.toString)
        context.trace.trace(reg.uriStore.toString)
      this

end HttpActor

object HttpActor:
  def apply(
      reg: Registry,
      isEntry: Boolean,
      bucketId: Option[String]
  ): Behavior[ApiCall] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timer =>
        new HttpActor(context, timer, reg, isEntry, bucketId)
      )
    )
