package org.maidagency.impl.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import org.parboiled2.RuleTrace.Run
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.*
import sttp.client3.pekkohttp.PekkoHttpBackend
import sttp.ws.WebSocket

enum HeartBeatSignal:
  case Beat

sealed class WebsocketHandler(
    context: ActorContext[HeartBeatSignal],
    timer: TimerScheduler[HeartBeatSignal],
    token: String
) extends AbstractBehavior[HeartBeatSignal](context):

  var webSocket: Option[WebSocket[Future]] = None

  startHandler()

  def startHandler() =
    val backend = PekkoHttpBackend()
    scribe.info("starting up websocket handler")
    basicRequest
      .get(uri"wss://gateway.discord.gg/?v=10&encoding=json")
      .response(asWebSocket(useWebSocket))
      .send(backend)
      .onComplete(_ => backend.close())

  def startHeartBeat(interval: Int) =
    scribe.info("heartbeat starting")
    timer.startTimerWithFixedDelay(
      msg = HeartBeatSignal.Beat,
      delay = interval.millis
    )

  def send(value: Obj) =
    scribe.info(s"sent message: $value")
    webSocket match
      case Some(ws) =>
        ws.sendText(value.toString)
      case None =>
        throw new RuntimeException("websocket not started yet")

  def receive() =
    webSocket match
      case Some(ws) =>
        ws.receiveText().map(handleMessage)
      case None =>
        throw new RuntimeException("websocket not started yet")

  def useWebSocket(ws: WebSocket[Future]): Future[Unit] =
    webSocket = Some(ws)
    for _ <- receive()
    yield ()

  private def handleMessage(message: String): Unit =
    import org.maidagency.impl.gateway.{GatewayPayload as Payload, *}

    scribe.info(s"got string message: $message")

    val json    = JsonParser(message, Format.Json)
    val payload = json.as[Payload]
    payload match
      case Payload(10, Some(HelloPayload(interval, _)), _, _) =>
        scribe.info(s"received heartbeat interval: $interval")
        startHeartBeat(interval)
      case Payload(11, None, _, _) =>
        scribe.info(s"heartbeat acknowledged")
      case _ =>
        scribe.info(s"received message: $message")

  override def onSignal: PartialFunction[Signal, Behavior[HeartBeatSignal]] =
    case PostStop =>
      scribe.info("killing Websocket handler")
      this

  override def onMessage(msg: HeartBeatSignal): Behavior[HeartBeatSignal] =
    msg match
      case HeartBeatSignal.Beat =>
        scribe.info("sending heartbeat over")
        send(obj("op" -> 1, "d" -> obj()))
        this

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[HeartBeatSignal] =
    scribe.info("websocket handler apply method called")
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
