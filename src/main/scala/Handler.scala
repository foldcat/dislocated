package org.maidagency.maidlib.handler

import fabric.*
import org.apache.pekko
import org.apache.pekko.actor.typed.*
import org.maidagency.maidlib.impl.websocket.websocket.WebsocketHandler
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import scala.concurrent.duration.*

class EventHandler(
    context: ActorContext[String],
    token: String
) extends AbstractBehavior[String](context):

  context.log.info("running event handler")

  // fire it up layer 2
  context.spawn(
    Behaviors
      .supervise(WebsocketHandler(token))
      .onFailure[Exception](
        SupervisorStrategy.restart
          .withLimit(3, 10.seconds)
      ),
    "websocket-handler-impl"
  )

  override def onMessage(msg: String): Behavior[String] =
    Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[String]] =
    case PostStop =>
      context.log.info("stopping handler")
      this

object EventHandler:
  def apply(token: String) =
    Behaviors.setup(context => new EventHandler(context, token))
