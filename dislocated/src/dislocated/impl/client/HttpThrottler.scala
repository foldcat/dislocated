package com.github.foldcat.dislocated.impl.client.httpthrottler

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.bucketexecutor.*
import com.github.foldcat.dislocated.impl.client.registry.*
import com.github.foldcat.dislocated.impl.client.registry.Registry
import com.github.foldcat.dislocated.impl.util.label.Label.genLabel
import com.github.foldcat.dislocated.impl.websocket.chan.Put.*
import org.apache.pekko
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.stream.*
import pekko.stream.scaladsl.*
import scala.concurrent.*
import scala.concurrent.duration.*

class HttpThrottler(
    context: ActorContext[ApiCall]
) extends AbstractBehavior[ApiCall](context):

  import ApiCall.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpThrottler])

  val registry = new Registry()

  val defaultExecutor = context.spawn(
    HttpActor(registry, true, None, None),
    genLabel("http-bucket-executor-default")
  )

  val queue = Source
    .queue[Call](1000)
    // discord global rate limit
    // TODO: user configable
    .throttle(50, 1.second)
    .toMat(
      Sink.foreach(call =>
        registry.route(call.uri) match
          case None =>
            defaultExecutor ! call
          case Some(value) =>
            value ! call
      )
    )(Keep.left)
    .run()

  override def onMessage(msg: ApiCall): Behavior[ApiCall] =
    msg match
      case call: Call =>
        queue !< call
      case _ =>
        throw new IllegalArgumentException("wrong call")
      // TDDO: custom exception
    this

  end onMessage

  override def onSignal: PartialFunction[Signal, Behavior[ApiCall]] =
    case PreRestart =>
      context.log.trace("restarting http funnel")
      this

end HttpThrottler

object HttpThrottler:
  def apply(): Behavior[ApiCall] =
    Behaviors.setup(context => new HttpThrottler(context))
