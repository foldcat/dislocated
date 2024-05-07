package org.maidagency.maidlib.impl.websocket.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.maidagency.maidlib.impl.websocket.chan.Put.*
import org.maidagency.maidlib.impl.websocket.heartbeat.*
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

enum SpawnHeartBeat:
  case Start(interval: Int)
  case Kill
  case SwapResumeCode(newCode: Int)

// HACK: for some reason, it is impossible to
// spawn the `HeartBeat` actor inside `WebsocketHandler.handleMessage`
// I assume it is because of whatever is with the `Sink`
// I bypassed the issue by using `HeartBeatSpawner` to spawn `HeartBeat`
// and proxy messages to it
class MessageProxy(
    context: ActorContext[SpawnHeartBeat],
    chan: BoundedSourceQueue[TextMessage]
) extends AbstractBehavior[SpawnHeartBeat](context):

  var heartbeatActor: Option[ActorRef[HeartBeatSignal]] = None

  context.log.info("starting heart beat spawner")

  def extract: ActorRef[HeartBeatSignal] =
    heartbeatActor match
      case None =>
        throw IllegalStateException("illegal access to heartbeat actor")
      case Some(value) =>
        value

  override def onMessage(msg: SpawnHeartBeat): Behavior[SpawnHeartBeat] =
    import HeartBeatSignal.*
    msg match
      case SpawnHeartBeat.Start(interval) =>
        heartbeatActor = Some(
          context.spawn(
            Behaviors
              .supervise(HeartBeat(chan, interval))
              .onFailure[Exception](SupervisorStrategy.restart),
            "heartbeat-actor"
          )
        )

      case SpawnHeartBeat.Kill =>
        context.log.info("proxying kill")
        extract ! Kill
      case SpawnHeartBeat.SwapResumeCode(newCode) =>
        context.log.info(s"proxying swap resume code to $newCode")
        extract ! SwapResumeCode(newCode)
    this
end MessageProxy

object MessageProxy:
  def apply(chan: BoundedSourceQueue[TextMessage]): Behavior[SpawnHeartBeat] =
    Behaviors.setup(context => new MessageProxy(context, chan))

sealed class WebsocketHandler(
    context: ActorContext[Nothing],
    token: String
) extends AbstractBehavior[Nothing](context):

  context.log.info("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  def handleMessage(message: String): Unit =
    import org.maidagency.maidlib.impl.websocket.gateway.{
      GatewayPayload as Payload,
      *
    }
    val json    = JsonParser(message, Format.Json)
    val payload = json.as[Payload]
    payload match
      case Payload(10, Some(HelloPayload(interval, _)), _, _) =>
        logger.info(s"just received heartbeat interval: $interval")
        spawner ! SpawnHeartBeat.Start(interval)
      case Payload(11, None, _, _) =>
        logger.info(s"heartbeat acknowledged")
      case _ =>
        logger.info(s"received message: $message")

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    Behaviors.unhandled

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

  val spawner = context.spawn(MessageProxy(queue), "heartbeat-spawner")

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[Nothing] =
    Behaviors.setup(context => new WebsocketHandler(context, token))
