package com.github.foldcat.dislocated.impl.util.oneoffexecutor

import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

enum OneOffExecutorEvent:
  case Kill

class OneOffExecutor(
    context: ActorContext[OneOffExecutorEvent],
    f: () => Any
) extends AbstractBehavior[OneOffExecutorEvent](context):
  override def onMessage(
      msg: OneOffExecutorEvent
  ): Behavior[OneOffExecutorEvent] =
    Behaviors.stopped
  override def onSignal
      : PartialFunction[Signal, Behavior[OneOffExecutorEvent]] =
    case PostStop =>
      context.log.trace("one off executor done")
      Behaviors.stopped

  context.log.trace("one off execution firing")

  try
    f()
  finally
    context.self ! OneOffExecutorEvent.Kill

object OneOffExecutor:
  /** async execute f
    *
    * @param f
    *   lambda to be executed
    * @return
    *   nothing
    */
  def apply(f: () => Any): Behavior[OneOffExecutorEvent] =
    Behaviors.setup(context => new OneOffExecutor(context, f))
