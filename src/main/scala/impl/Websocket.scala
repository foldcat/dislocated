package org.maidagency.impl.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
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
  implicit val system: ActorSystem[Nothing] = context.system

  val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message]:
        case message: TextMessage.Strict =>
          println(message.text)
        case other =>
          println(s"ignored: $other")
    // ignore other message types
  val webSocketFlow =
    Http().webSocketClientFlow(WebSocketRequest("wss://ws.postman-echo.com/raw"))
  //val outgoing = Source.single(TextMessage("hello world!"))
  val outgoing = Source.tick(1.second, 5.second, TextMessage("potato"))

  val (upgradeResponse, closed) =
    outgoing
      .viaMat(webSocketFlow)(
        Keep.right
      ) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(incoming)(Keep.both) // also keep the Future[Done]
      .run()

  val connected = upgradeResponse.flatMap: upgrade =>
      if upgrade.response.status == StatusCodes.SwitchingProtocols then
        Future.successful(Done)
      else
        throw new RuntimeException(
          s"Connection failed: ${upgrade.response.status}"
        )

  connected.onComplete(println)
  closed.foreach(_ => println("closed"))

  override def onMessage(msg: HeartBeatSignal): Behavior[HeartBeatSignal] =
    Behaviors.unhandled

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
