package org.maidagency.maidlib.client

import fabric.*
import org.apache.pekko
import org.maidagency.maidlib.impl.client.actor.*
import org.maidagency.maidlib.impl.client.actor.ApiCalls.*
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.http.scaladsl.model.*
import scala.compiletime.ops.boolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import HttpMethods.*

class Client[T](token: String, context: ActorContext[T]):
  private val versionNumber = 10
  val apiUrl                = s"https://discord.com/api/v$versionNumber"
  val handler               = context.spawn(HttpActor(), "http-actor")

trait ApiCall:
  def run: Future[Unit]

class GetChannel[T](channelID: String)(implicit client: Client[T])
    extends ApiCall:
  def run: Future[Unit] = ???

class CreateMessage[T](channelID: String)(implicit client: Client[T])
    extends ApiCall:
  def run: Future[Unit] = ???

class TestRequest[T]()(implicit client: Client[T]) extends ApiCall:
  def run: Future[Unit] =
    val promise: Promise[Unit] = Promise[Unit]()
    client.handler ! Call(
      HttpRequest(
        method = POST,
        uri = "https://httpbin.org/post",
        entity = HttpEntity(
          ContentTypes.`application/json`,
          obj("hi" -> 1).toString
        )
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
