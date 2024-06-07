package com.github.foldcat.dislocated.client

import com.github.foldcat.dislocated.impl.client.actor.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.ActorSystem
import pekko.http.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.headers.*
import pekko.http.scaladsl.unmarshalling.*
import pekko.http.scaladsl.Http
import scala.concurrent.Promise
import scala.util.*
import HttpMethods.*

def test[T](req: HttpRequest, promise: Promise[Any])(implicit
    system: ActorSystem[T]
) =
  implicit val executionContext = system.executionContext
  Http()
    .singleRequest(req)
    .onComplete:
      case Success(res) =>
        Unmarshal(res)
          .to[String]
          .onComplete:
            case Success(data) =>
              system.log.info(data)
              promise.success(())
            case Failure(exception) =>
              promise.failure(exception)
      case Failure(exception) =>
        system.log.error(exception.getMessage)

class Client[T](token: String, context: ActorContext[T]):
  private val versionNumber = 10
  val apiUrl                = s"https://discord.com/api/v$versionNumber"

  val authHeader = RawHeader("Authorization", s"Bot $token")

  val handler = context.spawn(
    Behaviors
      .supervise(HttpActor())
      .onFailure[Exception](
        SupervisorStrategy.restart
      ),
    "http-actor"
  )

// def submitRequest[P <: PURR, T](
//     req: HttpRequest,
//     target: PURRSum
// )(implicit client: Client[T]) =
//   val promise: Promise[P] = Promise[P]()
//   client.handler ! Call(
//     req,
//     promise,
//     target
//   )
//   promise.future

// trait ApiCall:
//   def run: Future[Unit]

class GetChannel[T](channelID: String)(implicit client: Client[T]):
  // def run: Future[EventData.Channel] =
  def run: Unit =
    HttpRequest(
      headers = List(client.authHeader),
      method = GET,
      uri = s"${client.apiUrl}/channels/$channelID"
    )
    ()

  /* TODO: idea
   *
   * implicit client: Client = Client("token here")
   *
   * CreateMessage // pulls data from implicit client
   *  .content("content here")
   *  .embeds(EmbedObject)
   *  .optionalConfiguration(???)
   *  .run // return future
   * */
