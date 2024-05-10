package org.maidagency.maidlib.impl.websocket.websocket

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
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
    intents: Vector[GatewayIntent]
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
    // should think of a better way soon
    Map(
      "op" -> 2.asJson,
      "d" ->
        Map(
          "token" -> token.asJson,
          "properties" ->
            Map(
              "os"      -> optsys.asJson,
              "browser" -> "maidlib".asJson,
              "device"  -> "maidlib".asJson
            ).asJson,
          "intents" -> intents.toIntent.toInt.asJson
        ).asJson
    ).asJson.toString

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

end MessageProxy

object MessageProxy:
  def apply(
      chan: BoundedSourceQueue[TextMessage],
      token: String,
      intents: Vector[GatewayIntent]
  ): Behavior[ProxySignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new MessageProxy(context, token, timers, chan, intents)
      )
    )

sealed class WebsocketHandler(
    context: ActorContext[Nothing],
    token: String,
    intents: Vector[GatewayIntent] // TODO: switch to Set
) extends AbstractBehavior[Nothing](context):

  context.log.info("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  def handleMessage(message: String): Unit =

    val json = parse(message) match
      case Left(value)  => throw new IllegalStateException("parse fail")
      case Right(value) => value

    val cursor: HCursor = json.hcursor

    extension [T](value: Either[DecodingFailure, T])
      def unwrap: T =
        value match
          case Left(value)  => throw IllegalStateException(value)
          case Right(value) => value

    cursor.downField("op").as[Int].unwrap match
      case 10 =>
        val interval =
          cursor
            .downField("d")
            .downField("heartbeat_interval")
            .as[Int]
            .unwrap
        logger.info(s"just received heartbeat interval: $interval")
        spawner ! ProxySignal.Start(interval)
      case 11 =>
        logger.info(s"heartbeat acknowledged")
      case 0 =>
        logger.info(s"gateway event received")
      case _ =>
        logger.info(s"received message: $message")
  end handleMessage

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] =
    case PreRestart =>
      context.log.info("restarting websocket actor")
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
    context.spawn(MessageProxy(queue, token, intents), "heartbeat-spawner")
  context.watch(spawner)

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String, intents: Vector[GatewayIntent]): Behavior[Nothing] =
    Behaviors.setup(context => new WebsocketHandler(context, token, intents))
