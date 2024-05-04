package org.maidagency.maidlib.impl.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.ws.*
import pekko.http.scaladsl.Http
import pekko.stream.*
import pekko.stream.scaladsl.*
import pekko.stream.QueueOfferResult.*
import pekko.Done
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

enum HeartBeatSignal:
  case Beat

sealed class WebsocketHandler(
    context: ActorContext[HeartBeatSignal],
    timer: TimerScheduler[HeartBeatSignal],
    token: String
) extends AbstractBehavior[HeartBeatSignal](context):

  implicit val system: ActorSystem[Nothing] = context.system

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  // this will be useful later for resuming connection
  var resumeCode: Option[Int] = None

  // enqueue `in` to `q`
  // handles all the cases
  extension [T](q: BoundedSourceQueue[T])
    infix def !<(in: T): Unit =
      q.offer(in) match
        case Enqueued =>
          logger.debug(s"shoved in $in")
        case Dropped =>
          logger.debug(s"dropped $in")
        case Failure(cause) =>
          val failedReason = s"offer failed: ${cause.getMessage}"
          logger.debug(failedReason)
          throw new IllegalStateException(failedReason)
        case QueueClosed =>
          logger.debug("queue is closed")
          throw new IllegalAccessError("queue already closed")

  private def handleMessage(message: String): Unit =
    import org.maidagency.maidlib.impl.gateway.{GatewayPayload as Payload, *}
    val json    = JsonParser(message, Format.Json)
    val payload = json.as[Payload]
    payload match
      case Payload(10, Some(HelloPayload(interval, _)), _, _) =>
        logger.info(s"received heartbeat interval: $interval")
        startHeartbeat(interval)
      case Payload(11, None, _, _) =>
        logger.info(s"heartbeat acknowledged")
      case _ =>
        logger.info(s"received message: $message")

  def startHeartbeat(interval: Int) =
    logger.info("starting heartbeat")
    beat
    timer.startTimerWithFixedDelay(
      msg = HeartBeatSignal.Beat,
      delay = interval.millis
    )

  // convinent shorthand
  // if resume code exist send it over
  def beat =
    val code =
      resumeCode match
        case None        => obj("d" -> Null)
        case Some(value) => obj("d" -> value)
    queue !< TextMessage(
      obj("op" -> 1)
        .merge(code)
        .toString
    )

  override def onMessage(msg: HeartBeatSignal): Behavior[HeartBeatSignal] =
    msg match
      case HeartBeatSignal.Beat =>
        logger.info("sending heartbeat")
        beat
        this

  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message]:
        case message: TextMessage.Strict =>
          logger.info(s"got ${message.text}")
          handleMessage(message.text)
        case other =>
          logger.info(s"ignored: $other")

  val webSocketFlow =
    Http().webSocketClientFlow(
      WebSocketRequest("wss://gateway.discord.gg/?v=10&encoding=json")
    )

  val src = Source
    .queue[TextMessage](100)
    .viaMat(webSocketFlow)(Keep.both)
    .toMat(incoming)(Keep.left)
    .run()

  // extract the queue
  val (queue, _) = src

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[HeartBeatSignal] =
    // Behaviors
    //   .supervise(WebsocketHandler(token))
    //   .onFailure[RuntimeException](
    //     SupervisorStrategy.restart
    //   )
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new WebsocketHandler(context, timers, token)
      )
    )
