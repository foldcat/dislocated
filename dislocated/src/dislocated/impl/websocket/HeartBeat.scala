package com.github.foldcat.dislocated.impl.websocket.heartbeat

import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import fabric.*
import java.util.concurrent.atomic.AtomicInteger
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.ws.*
import pekko.stream.*
import scala.concurrent.duration.*

enum HeartBeatSignal:
  case Beat
  case Kill
  case SwapResumeCode(newCode: Int)

class HeartBeat(
    context: ActorContext[HeartBeatSignal],
    timer: TimerScheduler[HeartBeatSignal],
    interval: Int,
    chan: BoundedSourceQueue[TextMessage],
    atom: AtomicInteger
) extends AbstractBehavior[HeartBeatSignal](context):

  var resumeCode: Option[Int] = None // TODO: populate this

  context.trace.info("starting heartbeat actor")

  beat

  timer.startTimerWithFixedDelay(
    msg = HeartBeatSignal.Beat,
    delay = interval.millis
  )

  def beat =
    context.trace.info("sending over heartbeat")
    val code = atom.get
    chan !< TextMessage(
      obj("op" -> 1, "d" -> code).toString
    )

  override def onMessage(
      msg: HeartBeatSignal
  ): Behavior[HeartBeatSignal] =
    msg match
      case HeartBeatSignal.Beat =>
        beat
        this
      case HeartBeatSignal.SwapResumeCode(newCode) =>
        resumeCode = Some(newCode)
        this
      case HeartBeatSignal.Kill =>
        Behaviors.stopped

  override def onSignal
      : PartialFunction[Signal, Behavior[HeartBeatSignal]] =
    case PostStop =>
      context.trace.info("stopping heartbeat")
      this
    case PreRestart =>
      context.trace.info("restarting heartbeat")
      this

end HeartBeat

object HeartBeat:
  def apply(
      chan: BoundedSourceQueue[TextMessage],
      interval: Int,
      atom: AtomicInteger
  ): Behavior[HeartBeatSignal] =
    Behaviors.setup(context =>
      Behaviors.withTimers(timers =>
        new HeartBeat(context, timers, interval, chan, atom)
      )
    )
