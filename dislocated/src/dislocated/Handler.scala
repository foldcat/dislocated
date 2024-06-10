package com.github.foldcat.dislocated.handler

import com.github.foldcat.dislocated.gatewayintents.GatewayIntent
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.impl.websocket.websocket.*
import com.github.foldcat.dislocated.objects.EventData.*
import fabric.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

enum EventHandlerSignals:
  case Kill

final class EventHandler[EventHandlerSignals](
    context: ActorContext[EventHandlerSignals],
    token: String,
    intents: Set[GatewayIntent],
    handler: (Events, Json) => Any
) extends AbstractBehavior[EventHandlerSignals](context):

  context.log.info("running event handler")

  final private val wssHandler = context.spawn(
    WebsocketHandler(token, intents, handler),
    genLabel("websocket-handler-impl")
  )

  context.watch(wssHandler)

  override def onMessage(
      msg: EventHandlerSignals
  ): Behavior[EventHandlerSignals] =
    msg match
      case EventHandlerSignals.Kill =>
        wssHandler ! WebsocketSignal.Kill
        Behaviors.stopped

  override def onSignal
      : PartialFunction[Signal, Behavior[EventHandlerSignals]] =
    case PostStop =>
      context.log.info("stopping handler")
      this
    case ChildFailed(_, ex) =>
      context.log.info("child failed")
      throw ex
      this

end EventHandler

object EventHandler:
  def apply(
      token: String,
      intents: Set[GatewayIntent],
      handler: (Events, Json) => Any
  ): Behavior[WebsocketSignal] =
    Behaviors.setup(context =>
      new EventHandler(context, token, intents, handler)
    )
