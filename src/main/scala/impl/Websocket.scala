package org.maidagency.impl.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import org.apache.pekko.actor.Cancellable
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
    token: String
) extends AbstractBehavior[HeartBeatSignal](context):

  var ticker: Option[Cancellable] = None

  val backend = PekkoHttpBackend()
  scribe.info("starting up websocket handler")

  basicRequest
    .get(uri"wss://gateway.discord.gg/?v=10&encoding=json")
    .response(asWebSocket(useWebSocket))
    .send(backend)
    .onComplete(_ => backend.close())

  // doesn't work, need fix
  def startHeartBeat(interval: Int) =
    scribe.info("heartbeat starting")
    Behaviors.withTimers[HeartBeatSignal]: timer =>
        timer.startTimerAtFixedRate(
          msg = HeartBeatSignal.Beat,
          interval = 1000.millis
        )
        this

  def useWebSocket(ws: WebSocket[Future]): Future[Unit] =
    def send(value: Obj) =
      scribe.info(s"sent message: $value")
      ws.sendText(value.toString)
    def receive() = ws.receiveText().map(handleMessage)
    for
      // _ <- send(obj("time" -> 1))
      _ <- receive()
      _ <- send(obj("op" -> 1, "d" -> obj()))
      _ <- receive()
    // _ <- receive() not allowed: more receive() calls than send calls will block forever
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
        scribe.info("got heartbeat")
        Behaviors.same

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[HeartBeatSignal] =
    scribe.info("websocket handler apply method called")
    // Behaviors
    //   .supervise(WebsocketHandler(token))
    //   .onFailure[RuntimeException](
    //     SupervisorStrategy.restart
    //   )
    Behaviors.setup(context => new WebsocketHandler(context, token))
