package org.maidagency.maidlib.impl.websocket.websocket

import fabric.*
import fabric.io.*
import fabric.rw.*
import org.apache.pekko
import org.maidagency.maidlib.impl.websocket.chan.Put.*
import org.maidagency.maidlib.impl.websocket.gateway.*
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
import scala.math.round
import scala.util.Random

enum ProxySignal:
  case Start(interval: Int)
  case Kill
  case Identify
  case SwapResumeCode(newCode: Int)

// HACK: it is impossible to spawn actor inside `Stream`s
// bypassed with a proxy actor
class MessageProxy(
    context: ActorContext[ProxySignal],
    token: String,
    timer: TimerScheduler[ProxySignal],
    chan: BoundedSourceQueue[TextMessage],
    intent: Vector[GatewayIntent]
) extends AbstractBehavior[ProxySignal](context):

  var heartbeatActor: Option[ActorRef[HeartBeatSignal]] = None

  // TODO: make this optional
  val optsys = System.getProperty("os.name").toLowerCase

  context.log.info("starting heart beat spawner")

  def extract: ActorRef[HeartBeatSignal] =
    heartbeatActor match
      case None =>
        throw IllegalStateException("illegal access to heartbeat actor")
      case Some(value) =>
        value

  val identifyJson =
    import GatewayIntent.*
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
          "intents" -> intent.toIntent.toInt
        )
    ).toString

  def awaitIdentify(interval: Int) =
    // better follow what discord told us to do
    val jitter   = Random.nextFloat
    val waitTime = round(jitter * interval)

    context.log.info(s"identifing after $waitTime ms")
    timer.startSingleTimer(ProxySignal.Identify, waitTime.millis)

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
        awaitIdentify(interval)

      case ProxySignal.Identify =>
        context.log.info("identifing")
        chan !< TextMessage(identifyJson)
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
      token: String,
      intent: Vector[GatewayIntent]
  ): Behavior[ProxySignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new MessageProxy(context, token, timers, chan, intent)
      )
    )

sealed class WebsocketHandler(
    context: ActorContext[Nothing],
    token: String,
    intent: Vector[GatewayIntent] // TODO: switch to Set
) extends AbstractBehavior[Nothing](context):

  context.log.info("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  // blame discord api if this throws exception
  extension (opt: Json)
    def getUnwrap(lookup: String) =
      opt.get(lookup) match
        case None =>
          throw IllegalStateException("failed to unwrap")
        case Some(value) =>
          value

  def handleMessage(message: String): Unit =
    val json = JsonParser(message, Format.Json)
    json.getUnwrap("op").asInt match
      case 10 =>
        val interval =
          json
            .getUnwrap("d")
            .getUnwrap("heartbeat_interval")
            .asInt
        logger.info(s"just received heartbeat interval: $interval")
        spawner ! ProxySignal.Start(interval)
      case 11 =>
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
    .queue[TextMessage](3) // find optional size for it
    .viaMat(webSocketFlow)(Keep.both)
    .toMat(incoming)(Keep.left)
    .run()

  // extract the queue
  val (queue, _) = src

  val spawner =
    context.spawn(MessageProxy(queue, token, intent), "heartbeat-spawner")
  context.watch(spawner)

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String, intent: Vector[GatewayIntent]): Behavior[Nothing] =
    Behaviors.setup(context => new WebsocketHandler(context, token, intent))
