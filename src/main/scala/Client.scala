package org.maidagency.maidlib.client

import org.apache.pekko
import pekko.http.scaladsl.model.*
import scala.compiletime.ops.boolean
import HttpMethods.*

class Client(token: String):

  private val versionNumber = 10

  private val apiUrl = s"https://discord.com/api/v$versionNumber"

  private val userAgent = ""

  case class AllowedMentions()

  case class Embed()

  case class MessageReference(
      messageID: String,
      channelID: String,
      guildID: String
  )

  def createMessage(
      channelId: String,
      content: Option[String] = None,
      tts: Boolean = false,
      embeds: Option[Vector[Embed]] = None,
      allowedMentions: Option[AllowedMentions] = None,
      messageReference: Option[MessageReference] = None,
      components: Option[String] = None, // TODO: populate the type
      stickerIds: Option[Vector[String]] = None,
      files: Option[String] = None // death
  ): Unit =
    HttpRequest(method = PATCH, uri = apiUrl)
end Client
