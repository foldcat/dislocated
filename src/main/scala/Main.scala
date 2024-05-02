package org.maidagency

import org.apache.pekko
import org.maidagency.handler.EventHandler
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scala.io.StdIn.readLine

enum MainSignal:
  case Start

class Main(context: ActorContext[MainSignal])
    extends AbstractBehavior[MainSignal](context):
  override def onMessage(msg: MainSignal): Behavior[MainSignal] =
    msg match
      case MainSignal.Start =>
        val rootHandler = context.spawn(EventHandler("123"), "event-handler")
        context.log.info(s"$rootHandler")
        this
  override def onSignal: PartialFunction[Signal, Behavior[MainSignal]] = 
    case PostStop => 
      context.log.info("stopping")
      this

object Main:
  def apply(): Behavior[MainSignal] =
    Behaviors.setup(context => new Main(context))

object StartUp extends App:
  val system = ActorSystem(Main(), "test-system")
  system ! MainSignal.Start
  readLine()
