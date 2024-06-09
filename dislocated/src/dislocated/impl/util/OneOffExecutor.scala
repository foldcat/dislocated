package com.github.foldcat.dislocated.impl.util.oneoffexecutor

import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

class OneOffExecutor(context: ActorContext[Nothing], f: () => Any)
    extends AbstractBehavior[Nothing](context):
  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
  context.log.info("one off execution firing")
  f()
  Behaviors.stopped

object OneOffExecutor:
  /** async execute f
    *
    * @param f
    *   lambda to be executed
    * @return
    *   nothing
    */
  def apply(f: () => Any): Behavior[Nothing] =
    Behaviors.setup(context => new OneOffExecutor(context, f))
