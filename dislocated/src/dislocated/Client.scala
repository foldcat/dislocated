package com.github.foldcat.dislocated.client

import com.github.foldcat.dislocated.impl.client.actor.*
import fabric.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.actor.typed.scaladsl.Behaviors
import pekko.http.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.headers.*
import scala.concurrent.*
import scala.concurrent.Promise
import HttpMethods.*

class Client[T](
    token: String,
    context: ActorContext[T],
    dispatcher: DispatcherSelector = DispatcherSelector.blocking()
):
  private val versionNumber = 10

  val apiUrl = s"https://discord.com/api/v$versionNumber"

  val authHeader = RawHeader("Authorization", s"Bot $token")

  val handler = context.spawn(
    Behaviors
      .supervise(HttpActor())
      .onFailure[Exception](
        SupervisorStrategy.restart
      ),
    "http-actor",
    dispatcher
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
  def run: Future[Json] =
    val promise = Promise[Json]()
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = GET,
        uri = s"${client.apiUrl}/channels/$channelID"
      ),
      promise
    )
    promise.future

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
