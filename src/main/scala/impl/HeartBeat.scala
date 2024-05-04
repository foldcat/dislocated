package org.maidagency.maidlib.impl.heartbeat

import fabric.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.ws.*
import pekko.http.scaladsl.Http
import pekko.stream.*
import pekko.stream.QueueOfferResult.*
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

enum HeartBeatSignal:
  case Beat
  case Kill
  case SwapResumeCode(newCode: Int)

class HeartBeat(
    context: ActorContext[HeartBeatSignal],
    timer: TimerScheduler[HeartBeatSignal],
    interval: Int,
    chan: BoundedSourceQueue[TextMessage]
) extends AbstractBehavior[HeartBeatSignal](context):

  import org.maidagency.maidlib.impl.chan.Put.*

  var resumeCode: Option[Int] = None

  context.log.info("starting heartbeat actor")

  beat

  timer.startTimerWithFixedDelay(
    msg = HeartBeatSignal.Beat,
    delay = interval.millis
  )

  def beat =
    context.log.info("sending over heartbeat")
    val code =
      resumeCode match
        case None        => obj("d" -> Null)
        case Some(value) => obj("d" -> value)
    chan !< TextMessage(
      obj("op" -> 1)
        .merge(code)
        .toString
    )

  override def onMessage(msg: HeartBeatSignal): Behavior[HeartBeatSignal] =
    msg match
      case HeartBeatSignal.Beat =>
        beat
        this
      case HeartBeatSignal.SwapResumeCode(newCode) =>
        resumeCode = Some(newCode)
        this
      case HeartBeatSignal.Kill =>
        Behaviors.stopped

  override def onSignal: PartialFunction[Signal, Behavior[HeartBeatSignal]] =
    case PostStop =>
      context.log.info("stopping heartbeat")
      this

end HeartBeat

object HeartBeat:
  def apply(
      chan: BoundedSourceQueue[TextMessage],
      interval: Int
  ): Behavior[HeartBeatSignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new HeartBeat(context, timers, interval, chan)
      )
    )
