package com.github.foldcat.dislocated.impl.util.oneoffexecutor

import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

class OneOffExecutor(
    context: ActorContext[Nothing],
    f: () => Any
) extends AbstractBehavior[Nothing](context):
  override def onMessage(
      msg: Nothing
  ): Behavior[Nothing] =
    Behaviors.unhandled
  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] =
    case PostStop =>
      context.log.info("one off executor done")
      Behaviors.stopped

  context.log.info("one off execution firing")

  try
    f()
  finally
    context.stop(context.self)

object OneOffExecutor:
  /** asynchronously execute lambda
    *
    * self terminates once the lambda finishes execution
    *
    * will self terminate no matter what due to finally
    *
    * @param f
    *   lambda to be executed
    * @return
    *   an behavior that can be spawned
    */
  def apply(f: () => Any): Behavior[Nothing] =
    Behaviors.setup(context => new OneOffExecutor(context, f))
