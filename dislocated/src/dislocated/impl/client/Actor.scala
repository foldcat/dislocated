package com.github.foldcat.dislocated.impl.client.actor

import com.github.foldcat.dislocated.impl.util.json.*
import com.github.foldcat.dislocated.objects.EventData.Events
import org.apache.pekko
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import ujson.*

class OneOffExecutor(context: ActorContext[Nothing], f: () => Any)
    extends AbstractBehavior[Nothing](context):
  override def onMessage(msg: Nothing): Behavior[Nothing] =
    this
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

enum ApiCall:
  case Call(
      effect: () => Unit,
      promise: Promise[Unit]
  )

class HttpActor(context: ActorContext[ApiCall])
    extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    ???

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.info("restarting http funnel")
      this
end HttpActor

object HttpActor:
  def apply(): Behavior[ApiCall] =
    Behaviors.setup(context => new HttpActor(context))
