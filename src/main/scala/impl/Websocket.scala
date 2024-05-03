package org.maidagency.impl.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.ws.*
import pekko.http.scaladsl.Http
import pekko.stream.scaladsl.*
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

  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  implicit val system: ActorSystem[Nothing] = context.system

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

  val (queue, _) = src

  private def handleMessage(message: String): Unit =
    import org.maidagency.impl.gateway.{GatewayPayload as Payload, *}
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
    timer.startTimerWithFixedDelay(
      msg = HeartBeatSignal.Beat,
      delay = interval.millis
    )

  override def onMessage(msg: HeartBeatSignal): Behavior[HeartBeatSignal] =
    msg match
      case HeartBeatSignal.Beat =>
        logger.info("sending heartbeat")
        queue.offer(TextMessage(obj("op" -> 1, "d" -> Null).toString))
        this

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
