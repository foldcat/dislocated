package org.maidagency.maidlib.impl.websocket.websocket

import java.util.concurrent.atomic.AtomicInteger
import org.apache.pekko
import org.maidagency.maidlib.impl.util.json.*
import org.maidagency.maidlib.impl.util.json.CustomPickle.*
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
import pekko.util.*
import pekko.Done
import scala.collection.mutable.ArrayBuffer
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
    intents: Set[GatewayIntent],
    atom: AtomicInteger
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
    ujson.Obj(
      "op" -> 2,
      "d" ->
        ujson.Obj(
          "token" -> token,
          "properties" ->
            ujson.Obj(
              "os"      -> optsys,
              "browser" -> "maidlib",
              "device"  -> "maidlib"
            ),
          "intents"  -> intents.toIntent.toInt,
          "compress" -> true
        )
    ).toString

  def awaitIdentify(interval: Int) =
    // better follow what discord told us to do
    // val jitter   = Random.nextFloat
    val jitter   = 0.0
    val waitTime = round(jitter * interval)

    context.log.info(s"identifing after $waitTime ms")
    timer.startSingleTimer(ProxySignal.Identify, waitTime.millis)

  override def onMessage(msg: ProxySignal): Behavior[ProxySignal] =
    import HeartBeatSignal.*
    msg match
      case ProxySignal.Start(interval) =>
        heartbeatActor = Some(
          context.spawn(
            HeartBeat(chan, interval, atom),
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
      intents: Set[GatewayIntent],
      atom: AtomicInteger
  ): Behavior[ProxySignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new MessageProxy(
          context,
          token,
          timers,
          chan,
          intents,
          atom
        )
      )
    )

sealed class WebsocketHandler(
    context: ActorContext[Nothing],
    token: String,
    intents: Set[GatewayIntent]
) extends AbstractBehavior[Nothing](context):

  context.log.info("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  val resumeCode                = new AtomicInteger(0)
  var resumeUrl: Option[String] = None

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  def handleEvent(message: String, data: ujson.Value): Unit =
    import org.maidagency.maidlib.objects.*

    // given ReadWriter[MessageCreateEvent] = macroRW[MessageCreateEvent]
    // given ReadWriter[Events] = readwriter[ujson.Value].bimap[Events](
    //   { case mc: MessageCreateEvent =>
    //     println(mc)
    //     writeJs(mc).obj += ("type" -> writeJs("MessageCreateEvent"))
    //   // case t => throw new IllegalArgumentException(s"Unknown type: $t")
    //   },
    //   json =>
    //     json("type").str match
    //       case "IntWrapper" => read[MessageCreateEvent](json)
    //       case t => throw new IllegalArgumentException(s"Unknown type: $t"),
    // )

    try
      message match
        case "MESSAGE_CREATE" =>
          logger.info("got message create event")
          logger.info(
            s"got message create event: ${CustomPickle.read[MessageCreateEvent](data)}"
          )
          logger.info("end")
        case "READY" =>
          val newUrl = data("resume_gateway_url").str
          logger.info(s"ready, new gateway url: $newUrl")
          resumeUrl = Some(newUrl)
        case _ =>
          logger.info("unhandled event caught")
    catch case e: Exception => e.printStackTrace
  end handleEvent

  def handleMessage(message: String): Unit =

    val json = ujson.read(message)

    val sequenceCode = json("s")
    if sequenceCode != ujson.Null then
      val newCode = sequenceCode.num.toInt
      logger.info(s"resume code swap to $newCode")
      resumeCode.set(newCode)

    json("op").num match
      case 10 =>
        val interval =
          json("d")("heartbeat_interval").num.toInt
        logger.info(s"just received heartbeat interval: $interval")
        spawner ! ProxySignal.Start(interval)
      case 11 =>
        logger.info(s"heartbeat acknowledged")
      case 0 =>
        logger.info(s"gateway event received")
        handleEvent(json("t").str, json("d"))
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
          logger.info("got text message")
          logger.info(s"got ${message.text}")
          handleMessage(message.text)
        case message: BinaryMessage =>
          // this is going to make me mald
          logger.info("got binary message")
          // TODO: find optimal value of the buffer size
          val bufferSize = message.getStrictData.size * 10
          message.dataStream
            .via(Compression.inflate(bufferSize))
            .runWith(
              Sink.foreach((str: ByteString) =>
                val output = str.utf8String
                logger.info(s"got $output")
                handleMessage(output)
              )
            )
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
    context.spawn(
      MessageProxy(queue, token, intents, resumeCode),
      "heartbeat-spawner"
    )
  context.watch(spawner)

end WebsocketHandler

object WebsocketHandler:
  def apply(token: String, intents: Set[GatewayIntent]): Behavior[Nothing] =
    Behaviors.setup(context => new WebsocketHandler(context, token, intents))
