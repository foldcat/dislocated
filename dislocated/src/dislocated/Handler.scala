package com.github.foldcat.dislocated.handler

import com.github.foldcat.dislocated.gatewayintents.GatewayIntent
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.impl.websocket.websocket.*
import com.github.foldcat.dislocated.objects.EventData.*
import fabric.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*

/** signals one can send to [[com.github.foldcat.dislocated.handler]]
  *
  * case Kill terminates said actor and disconnect from Discord
  * websocket
  */
enum EventHandlerSignals:
  case Kill

/** main entry point to listen to Discord API
  *
  * ### overview
  *
  * EventHandler is an actor, simply pass in all the parameters and
  * spawn it, then it will listen to the Discord websocket
  *
  * the handler function is executed asynchronously in the actor
  * [[com.github.foldcat.dislocated.impl.util.oneoffexecutor]] making
  * sure it isn't hogged
  *
  * @example
  *   for the example below: keep in mind you must provide the
  *   required implicits to the Discord API calls
  *   {{{
  * def token: String = "token here"
  * def intents: Set[GatewayIntent] = Set(ALL)
  *
  * // define the handler
  * def handler(event: Events, json: Json): Any =
  *   event match
  *     case Unimplemented() =>
  *     case obj: MessageCreateEvent =>
  *       obj.content match
  *         case None =>
  *         case Some("!ping") =>
  *           CreateMessage(obj.channelId)
  *             .content("pong!")
  *             .run
  *     case Some(value) =>
  *
  * // spawn the EventHandler
  * context.spawn(
  *   Behaviors
  *     .supervise(
  *       EventHandler(token, intents, handler)
  *     )
  *     .onFailure[WebsocketFailure](
  *       SupervisorStrategy.restart
  *         .withLimit(maxNrOfRetries = 5, withinTimeRange = 10.seconds)
  *     ),
  *     "core-handler"
  * )
  *   }}}
  *
  * should you want to terminate the connection, send EventHandler the
  * signal
  * [[package com.github.foldcat.dislocated.handler.EventHandlerSignals.Kill]]
  * in order to terminate the connection alongside said actor
  *
  * @param context
  *   actor context
  * @param token
  *   Discord bot token
  * @param intents
  *   gatway intent, see also
  *   [[com.github.foldcat.dislocated.gatewayintents]]
  * @param handler
  *   a method or function that is executed once said EventHandler
  *   receive any event payload from Discord websocket
  *
  * @throws WebsocketFailure
  *   actor will throw WebsocketFailure from
  *   [[com.github.foldcat.dislocated.impl.util.customexception]]: you
  *   are responsible for supervising this actor
  */
final class EventHandler[EventHandlerSignals](
    context: ActorContext[EventHandlerSignals],
    token: String,
    intents: Set[GatewayIntent],
    handler: (Events, Json) => Any
) extends AbstractBehavior[EventHandlerSignals](context):

  context.log.trace("running event handler")

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
      context.log.trace("stopping handler")
      this
    case ChildFailed(_, ex) =>
      context.log.trace("child failed")
      throw ex
      this

end EventHandler

object EventHandler:
  /** @param context
    *   actor context
    * @param token
    *   Discord bot token
    * @param intents
    *   gatway intent, see also
    *   [[com.github.foldcat.dislocated.gatewayintents]]
    * @param handler
    *   a method or function that is executed once said EventHandler
    *   receive any event payload from Discord websocket
    * @return
    *   a behavior that can be spawned by pekko
    */
  def apply(
      token: String,
      intents: Set[GatewayIntent],
      handler: (Events, Json) => Any
  ): Behavior[EventHandlerSignals] =
    Behaviors.setup(context =>
      new EventHandler(context, token, intents, handler)
    )
