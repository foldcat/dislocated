package com.github.foldcat.dislocated.impl.websocket.websocket

import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import com.github.foldcat.dislocated.impl.websocket.gateway.*
import com.github.foldcat.dislocated.impl.websocket.heartbeat.*
import com.github.foldcat.dislocated.objects.EventData
import fabric.*
import fabric.filter.*
import fabric.io.*
import fabric.rw.*
import java.util.concurrent.atomic.AtomicInteger
import org.apache.pekko
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.ws.*
import pekko.http.scaladsl.Http
import pekko.stream.*
import pekko.stream.scaladsl.*
import pekko.util.*
import pekko.Done
import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.math.round
// import scala.util.Random

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
    obj(
      "op" -> 2,
      "d" ->
        obj(
          "token" -> token,
          "properties" ->
            obj(
              "os"      -> optsys,
              "browser" -> "dislocated",
              "device"  -> "dislocated"
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
    intents: Set[GatewayIntent],
    eventQueue: BoundedSourceQueue[EventData.EventData]
) extends AbstractBehavior[Nothing](context):

  context.log.info("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  val resumeCode                = new AtomicInteger(0)
  var resumeUrl: Option[String] = None

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  def handleEvent(message: String, data: Json): Unit =
    import com.github.foldcat.dislocated.objects.EventData.*

    try
      message match
        case "MESSAGE_CREATE" =>
          // logger.info(processedData)
          // val y = MessageCreateEvent(
          //   "a",
          //   "a",
          //   User("s", "s", "a", Some("a"), Some("a")),
          //   Some("a"),
          //   "a",
          //   Some("a"),
          //   false,
          //   false,
          //   false,
          //   1
          // )
          // logger.info(CustomPickle.write(y))
          val parsed: EventData.Events =
            val json = SnakeToCamelFilter(
              data,
              JsonPath.empty
            ) match
              case Some(value) => value
              case None =>
                throw new IllegalStateException(
                  "fail to parse json in snake case"
                )

            json.as[MessageCreateEvent]

          eventQueue !< (parsed, data)

          logger.info("got message create event")
        case "READY" =>
          val newUrl = data("resume_gateway_url").asString
          logger.info(s"ready, new gateway url: $newUrl")
          resumeUrl = Some(newUrl)
        case _ =>
          logger.info("unhandled event caught")
          eventQueue !< (EventData.Unimplemented(), data)

    catch case e: Exception => e.printStackTrace
    end try
  end handleEvent

  def handleMessage(message: String): Unit =

    val json = JsonParser(message, Format.Json)

    val sequenceCode = json("s")
    if sequenceCode != fabric.Null then // TODO: fix
      val newCode = sequenceCode.asInt
      logger.info(s"resume code swap to $newCode")
      resumeCode.set(newCode)

    json("op").asInt match
      case 10 =>
        val interval =
          json("d")("heartbeat_interval").asInt
        logger.info(s"just received heartbeat interval: $interval")
        spawner ! ProxySignal.Start(interval)
      case 11 =>
        logger.info("heartbeat acknowledged")
      case 0 =>
        logger.info("gateway event received")
        handleEvent(json("t").asString, json("d"))
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

  val queue = Source
    .queue[TextMessage](3) // find optional size for it
    .viaMat(webSocketFlow)(Keep.left)
    .toMat(incoming)(Keep.left)
    .run()

  val spawner =
    context.spawn(
      MessageProxy(queue, token, intents, resumeCode),
      "heartbeat-spawner"
    )
  context.watch(spawner)

end WebsocketHandler

object WebsocketHandler:
  def apply(
      token: String,
      intents: Set[GatewayIntent],
      queue: BoundedSourceQueue[EventData.EventData]
  ): Behavior[Nothing] =
    Behaviors.setup(context =>
      new WebsocketHandler(context, token, intents, queue)
    )
