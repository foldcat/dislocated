package org.maidagency.impl.websocket

import fabric.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import sttp.client3.*
import sttp.client3.pekkohttp.PekkoHttpBackend
import sttp.ws.WebSocket

sealed class WebsocketHandler(
    context: ActorContext[Nothing],
    token: String
) extends AbstractBehavior[Nothing](context):

  val backend = PekkoHttpBackend()
  scribe.info("starting up websocket handler")

  basicRequest
    .get(uri"wss://ws.postman-echo.com/raw")
    .response(asWebSocket(useWebSocket))
    .send(backend)
    .onComplete(_ => backend.close())

  def useWebSocket(ws: WebSocket[Future]): Future[Unit] =
    def send(value: Obj) = ws.sendText(value.toString)
    def receive()        = ws.receiveText().map(t => scribe.info(s"got $t"))
    for
      _ <- send(obj("time" -> 1))
      _ <- send(obj("time" -> 2))
      _ <- receive()
      _ <- receive()
    yield ()

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] =
    case PostStop =>
      scribe.info("killing Websocket handler")
      this

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this


end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[Nothing] =
    scribe.info("websocket handler apply method called")
    // Behaviors
    //   .supervise(WebsocketHandler(token))
    //   .onFailure[RuntimeException](
    //     SupervisorStrategy.restart
    //   )
    Behaviors.setup(context => new WebsocketHandler(context, token))
