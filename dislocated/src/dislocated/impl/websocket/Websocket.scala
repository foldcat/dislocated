package com.github.foldcat.dislocated.impl.websocket.websocket

import com.github.foldcat.dislocated.gatewayintents.*
import com.github.foldcat.dislocated.impl.util.customexception.*
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.impl.util.oneoffexecutor.*
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

enum WebsocketSignal:
  case Kill
  case Exec(
      handler: (EventData.Events, Json) => Any,
      event: EventData.Events,
      data: Json
  )
  case Except(
      e: Throwable
  )
  case Identify
  case StartHeartBeat(interval: Int)
  case SwapResumeCode(newCode: Int)
  case KillHeartBeat

sealed class WebsocketHandler(
    context: ActorContext[WebsocketSignal],
    timer: TimerScheduler[WebsocketSignal],
    token: String,
    intents: Set[GatewayIntent],
    handler: (EventData.Events, Json) => Any
) extends AbstractBehavior[WebsocketSignal](context):

  context.log.trace("starting websocket handler")

  implicit val system: ActorSystem[Nothing] = context.system

  val resumeCode                = new AtomicInteger(0)
  var resumeUrl: Option[String] = None

  // slf4j
  val logger = LoggerFactory.getLogger(classOf[WebsocketHandler])

  final private var heartbeatActor
      : Option[ActorRef[HeartBeatSignal]] = None

  // override this
  val optsys = System.getProperty("os.name").toLowerCase

  final private def getHeartBeatActor: ActorRef[HeartBeatSignal] =
    heartbeatActor match
      case None =>
        throw WebsocketFailure(
          "illegal access to heartbeat actor"
        )
      case Some(value) =>
        value

  final private val identifyJson =
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

  final private def awaitIdentify(interval: Int) =
    // better follow what discord told us to do
    // val jitter   = Random.nextFloat
    val jitter   = 0.0
    val waitTime = round(jitter * interval)

    context.log.trace(s"identifing after $waitTime ms")
    timer.startSingleTimer(WebsocketSignal.Identify, waitTime.millis)

  def handleEvent(message: String, data: Json): Unit =
    import com.github.foldcat.dislocated.objects.EventData.*

    try
      message match
        case "MESSAGE_CREATE" =>
          // logger.trace(processedData)
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
          // logger.trace(CustomPickle.write(y))
          val parsed: EventData.Events =
            val json = SnakeToCamelFilter(
              data,
              JsonPath.empty
            ) match
              case Some(value) => value
              case None =>
                throw WebsocketFailure(
                  "fail to parse json in snake case"
                )

            json.as[EventData.MessageCreateEvent]

          context.self ! WebsocketSignal.Exec(
            handler,
            parsed,
            data
          )

          logger.trace("got message create event")
        case "READY" =>
          val newUrl = data("resume_gateway_url").asString
          logger.trace(s"ready, new gateway url: $newUrl")
          resumeUrl = Some(newUrl)
        case _ =>
          logger.trace("unhandled event caught")
          context.self ! WebsocketSignal.Exec(
            handler,
            EventData.Unimplemented(),
            data
          )

    catch case e: Exception => e.printStackTrace

    end try

  end handleEvent

  def handleMessage(message: String): Unit =

    val json = JsonParser(message, Format.Json)

    val sequenceCode = json("s")
    if sequenceCode != fabric.Null then
      val newCode = sequenceCode.asInt
      logger.trace(s"resume code swap to $newCode")
      resumeCode.set(newCode)

    json("op").asInt match
      case 10 =>
        val interval =
          json("d")("heartbeat_interval").asInt
        logger.trace(s"just received heartbeat interval: $interval")
        context.self ! WebsocketSignal.StartHeartBeat(interval)
      case 11 =>
        logger.trace("heartbeat acknowledged")
      case 0 =>
        logger.trace("gateway event received")
        handleEvent(json("t").asString, json("d"))
      case _ =>
        logger.trace(s"received message: $message")

  end handleMessage

  override def onMessage(
      msg: WebsocketSignal
  ): Behavior[WebsocketSignal] =
    import WebsocketSignal.*
    msg match
      case Kill =>
        wsRef ! Done
        Behaviors.stopped
      case Exec(handler, event, data) =>
        context.spawn(
          OneOffExecutor(() => handler(event, data)),
          genLabel("one-off-executor")
        )
        this
      case Except(e) =>
        throw WebsocketFailure(
          s"msg: ${e.getMessage} \n ${e.getStackTrace}"
        )
      case StartHeartBeat(interval) =>
        heartbeatActor = Some(
          context.spawn(
            HeartBeat(wsRef, interval, resumeCode),
            genLabel("heartbeat-actor")
          )
        )
        context.watch(getHeartBeatActor)
        awaitIdentify(interval)
        this

      case Identify =>
        context.log.trace("identifing")
        wsRef ! TextMessage(identifyJson)
        this
      case KillHeartBeat =>
        context.log.trace("proxying kill")
        getHeartBeatActor ! HeartBeatSignal.Kill
        this
      case SwapResumeCode(newCode) =>
        context.log.trace(s"proxying swap resume code to $newCode")
        getHeartBeatActor ! HeartBeatSignal.SwapResumeCode(newCode)
        this

    end match

  end onMessage

  override def onSignal
      : PartialFunction[Signal, Behavior[WebsocketSignal]] =
    case PreRestart =>
      context.log.trace("restarting websocket actor")
      this
    case ChildFailed(_, ex) =>
      context.log.trace("child failed")
      throw ex
      this

  val incoming: Sink[Message | Unit, Future[Done]] =
    Sink.foreach[Message | Unit]:
        case message: TextMessage.Strict =>
          logger.trace("got text message")
          logger.trace(s"got ${message.text}")
          handleMessage(message.text)
        case message: BinaryMessage =>
          // this is going to make me mald
          logger.trace("got binary message")
          val bufferSize = message.getStrictData.size * 10
          message.dataStream
            .via(Compression.inflate(bufferSize))
            .runWith(
              Sink.foreach((str: ByteString) =>
                val output = str.utf8String
                logger.trace(s"got $output")
                handleMessage(output)
              )
            )
        case other =>
          logger.trace(s"ignored: $other")

  val webSocketFlow =
    Http().webSocketClientFlow(
      WebSocketRequest("wss://gateway.discord.gg/?v=10&encoding=json")
    )

  val ((wsRef, upgradedResponse), close) = Source
    .actorRef(
      completionMatcher = { case Done =>
        CompletionStrategy.immediately
      },
      failureMatcher = PartialFunction.empty,
      bufferSize = 3,
      overflowStrategy = OverflowStrategy.dropHead
    )
    .viaMat(webSocketFlow)(Keep.both)
    .toMat(incoming)(Keep.both)
    .run()

end WebsocketHandler

object WebsocketHandler:
  def apply(
      token: String,
      intents: Set[GatewayIntent],
      handler: (EventData.Events, Json) => Any
  ): Behavior[WebsocketSignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new WebsocketHandler(context, timers, token, intents, handler)
      )
    )
