package org.maidagency.maidlib.client

import org.apache.pekko
import pekko.http.scaladsl.model.*
import scala.compiletime.ops.boolean
import HttpMethods.*

class Client(token: String):
  private val versionNumber = 10
  val apiUrl                = s"https://discord.com/api/v$versionNumber"

case class CreateMessageData(content: Option[String])
class CreateMessage(channelID: String)(implicit client: Client):
  CreateMessageData(None)
  def content(content: String) = CreateMessageData(Some(content))

  /* TODO: idea
   * implicit client: Client = Client("token here")
   *
   * CreateMessage // pulls data from implicit client
   *  .content("content here")
   *  .embeds(EmbedObject)
   *  .optionalConfiguration(???)
   *  .run // execute now or
   *  .runFuture // return execution as Future
   * */
