package org.maidagency.maidlib.handler

import fabric.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import org.maidagency.impl.websocket.WebsocketHandler
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

class EventHandler(
    context: ActorContext[String],
    token: String
) extends AbstractBehavior[String](context):

  context.log.info("running event handler")

  // fire it up layer 2
  context.spawn(WebsocketHandler(token), "websocket-handler-impl")

  override def onMessage(msg: String): Behavior[String] =
    Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[String]] =
    case PostStop =>
      context.log.info("stopping handler")
      this

object EventHandler:
  def apply(token: String) =
    Behaviors.setup(context => new EventHandler(context, token))
