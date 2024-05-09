package org.maidagency.maidlib.impl.client.actor

import fabric.*
import org.apache.pekko
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success

enum ApiCalls:
  case Call(
      req: HttpRequest,
      promise: Promise[Unit]
  )

class HttpActor(context: ActorContext[ApiCalls])
    extends AbstractBehavior[ApiCalls](context):

  import ApiCalls.*

  implicit val system: ActorSystem[Nothing] = context.system

  val logger = LoggerFactory.getLogger(classOf[HttpActor])

  override def onMessage(msg: ApiCalls): Behavior[ApiCalls] =
    msg match
      case Call(req, promise) =>
        Http()
          .singleRequest(req)
          .onComplete:
            case Success(res) =>
              Unmarshal(res)
                .to[String]
                .map(logger.info(_))
              promise.success(())
            case Failure(cause) =>
              throw cause
        this

  override def onSignal: PartialFunction[Signal, Behavior[ApiCalls]] =
    case PreRestart =>
      context.log.info("restarting http funnel")
      this

object HttpActor:
  def apply(): Behavior[ApiCalls] =
    Behaviors.setup(context => new HttpActor(context))
