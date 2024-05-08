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

enum ProxySignal:
  case Start(interval: Int)
  case Kill
  case SwapResumeCode(newCode: Int)

// HACK: for some reason, it is impossible to
// spawn the `HeartBeat` actor inside `WebsocketHandler.handleMessage`
// I assume it is because of whatever is with the `Sink`
// I bypassed the issue by using `HeartBeatSpawner` to spawn `HeartBeat`
// and proxy messages to it
class MessageProxy(
    context: ActorContext[ProxySignal],
    token: String,
    timer: TimerScheduler[ProxySignal],
    chan: BoundedSourceQueue[TextMessage]
) extends AbstractBehavior[ProxySignal](context):

  var heartbeatActor: Option[ActorRef[HeartBeatSignal]] = None

  context.log.info("starting heart beat spawner")

  def extract: ActorRef[HeartBeatSignal] =
    heartbeatActor match
      case None =>
        throw IllegalStateException("illegal access to heartbeat actor")
      case Some(value) =>
        value

  def identify() =

    // TODO: make this optional
    val optsys = System.getProperty("os.name").toLowerCase

    val identifyJson =
      obj(
        "op" -> 2,
        "d" ->
          obj(
            "token" -> token,
            "properties" ->
              obj(
                "os"      -> optsys,
                "browser" -> "maidlib",
                "device"  -> "maidlib"
              ),
            "intents" -> ""
          )
      )

  override def onMessage(msg: ProxySignal): Behavior[ProxySignal] =
    import HeartBeatSignal.*
    msg match
      case ProxySignal.Start(interval) =>
        heartbeatActor = Some(
          context.spawn(
            HeartBeat(chan, interval),
            "heartbeat-actor"
          )
        )
        context.watch(extract)

      case ProxySignal.Kill =>
        context.log.info("proxying kill")
        extract ! Kill
      case ProxySignal.SwapResumeCode(newCode) =>
        context.log.info(s"proxying swap resume code to $newCode")
        extract ! SwapResumeCode(newCode)
    this

  override def onSignal: PartialFunction[Signal, Behavior[ProxySignal]] =
    case PostStop =>
      context.log.info("message proxy terminating")
      this
    case _: Terminated =>
      context.log.error("heartbeat actor terminated")
      throw new IllegalStateException("heartbeat actor is dead")
      this

end MessageProxy

object MessageProxy:
  def apply(
      chan: BoundedSourceQueue[TextMessage],
      token: String
  ): Behavior[ProxySignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new MessageProxy(context, token, timers, chan)
      )
    )

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
        spawner ! ProxySignal.Start(interval)
      case Payload(11, None, _, _) =>
        logger.info(s"heartbeat acknowledged")
      case _ =>
        logger.info(s"received message: $message")

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] =
    case PreRestart =>
      context.log.info("restarting websocket actor")
      this
    case _: Terminated =>
      context.log.error("proxy terminated")
      throw new IllegalStateException("proxy is dead")
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

  val spawner = context.spawn(MessageProxy(queue, token), "heartbeat-spawner")
  context.watch(spawner)

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String): Behavior[Nothing] =
    Behaviors.setup(context => new WebsocketHandler(context, token))
