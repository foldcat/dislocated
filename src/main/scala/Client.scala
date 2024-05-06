package org.maidagency.maidlib.client

import org.apache.pekko
import pekko.http.scaladsl.model.*
import scala.compiletime.ops.boolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import HttpMethods.*

class Client(token: String):
  private val versionNumber = 10
  val apiUrl                = s"https://discord.com/api/v$versionNumber"

trait ApiCall:
  def run(): Future[Try[Unit]]

class GetChannel(channelID: String)(implicit client: Client) extends ApiCall:
  def run(): Future[Try[Unit]] = ???

class CreateMessage(channelID: String)(implicit client: Client) extends ApiCall:
  def run(): Future[Try[Unit]] = ???

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
