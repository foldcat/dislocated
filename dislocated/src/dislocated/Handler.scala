package com.github.foldcat.dislocated.handler

import com.github.foldcat.dislocated.gatewayintents.GatewayIntent
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.impl.websocket.websocket.*
import com.github.foldcat.dislocated.objects.EventData.*
import fabric.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

abstract class EventHandler[T](
    context: ActorContext[T]
) extends AbstractBehavior[T](context):

  context.log.info("running event handler")

  def token: String

  def intents: Set[GatewayIntent]

  def handler: (Events, Json) => Any

  final val wssHandler = context.spawn(
    WebsocketHandler(token, intents, handler),
    genLabel("websocket-handler-impl")
  )

  final def kill =
    wssHandler ! WebsocketSignal.Kill

  context.watch(wssHandler)

  // override def onMessage(msg: T): Behavior[T] =
  //   Behaviors.unhandled
  //
  // override def onSignal: PartialFunction[Signal, Behavior[T]] =
  //   case PostStop =>
  //     context.log.info("stopping handler")
  //     this
end EventHandler
