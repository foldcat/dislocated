package org.maidagency

import org.apache.pekko
import org.maidagency.handler.EventHandler
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scribe.*

enum MainSignal:
  case Start

class Main(context: ActorContext[MainSignal])
    extends AbstractBehavior[MainSignal](context):
  override def onMessage(msg: MainSignal): Behavior[MainSignal] =
    msg match
      case MainSignal.Start =>
        val rootHandler = context.spawn(EventHandler("123"), "event-handler")
        scribe.info(s"$rootHandler")
        this

object Main:
  def apply(): Behavior[MainSignal] =
    Behaviors.setup(context => new Main(context))

object StartUp extends App:
  val system = ActorSystem(Main(), "test-system")
  system ! MainSignal.Start
