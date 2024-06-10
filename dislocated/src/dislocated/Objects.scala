package com.github.foldcat.dislocated.objects

import fabric.*
import fabric.rw.*

/*  Partial + Usable Response Representation
 *  Partial: Don't expect more data than absolutely required
 *  Usable: TODO We add more data optionally if it makes life easier for the end user
 *  Response: This is the data on the receiving end, coming from Discord
 *  Representation: Define a format that can be parsed from JSON but is more type-safe
 */

object EventData:

  sealed trait Events

  sealed trait EventObject

  type EventData = (Events, Json)

  case class Unimplemented() extends Events

  case class MessageCreateEvent(
      id: String,
      channelId: String,
      author: User,
      content: Option[String] = None,
      timestamp: String,
      editedTimestamp: Option[String] = None,
      tts: Boolean,
      mentionEveryone: Boolean,
      // mentionRoles: Seq[Role],
      // embeds: Option[Embed] = None,
      pinned: Boolean,
      `type`: Int, // TODO: convert message type to real human readable form
      guildId: Option[String] = None
      // member: Option[GuildMember] = None,
      // mentions: Seq[UserWithMember]
  ) extends Events derives RW

  case class GuildMember(
      // roles: Option[Vector[String]] = None,
      joinedAt: String,
      deaf: Boolean,
      mute: Boolean,
      flags: Int
  ) extends EventObject derives RW

  case class User(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None
  ) extends EventObject derives RW

  case class UserWithMember(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None,
      member: GuildMember
  ) extends EventObject derives RW

  case class Member() extends EventObject

  case class Role() extends EventObject

  case class ChannelMention() extends EventObject

  case class Embed(
      title: Option[String] = None,
      `type`: Option[String] = None,
      description: Option[String] = None,
      url: Option[String] = None,
      timestamp: Option[String] = None,
      color: Option[Int] = None
      // footer: Option[EmbedFooter] = None,
      // image: Option[EmbedImage] = None,
      // thumbnail: Option[EmbedThumbnail] = None,
      // video: Option[EmbedVideo] = None,
      // provider: Option[EmbedProvider] = None,
      // author: Option[EmbedAuthor] = None,
      // fields: Option[List[EmbedField]] = None
  ) extends EventObject derives RW

  case class Reaction() extends EventObject

  case class MessageActivity() extends EventObject

  case class Application() extends EventObject

  case class MessageReference() extends EventObject

  case class MessageInteractionMetadata() extends EventObject

  case class MessageInteraction() extends EventObject

  case class Channel(
      id: String,
      `type`: Int,
      guildId: Option[String] = None,
      position: Option[Int] = None,
      name: Option[String] = None,
      topic: Option[String] = None,
      nsfw: Option[Boolean] = None,
      lastMessageId: Option[String] = None
  ) extends EventObject derives RW

  case class StickerItem() extends EventObject

  case class Sticker() extends EventObject

  case class RoleSubscriptionData() extends EventObject

  case class ResolvedData() extends EventObject

  case class Poll() extends EventObject

  case class Message(
      id: String,
      channelId: String,
      // author: User,
      content: Option[String] = None,
      timestamp: String,
      editedTimestamp: Option[String] = None,
      tts: Boolean,
      mentionEveryone: Boolean,
      // mentions: Vector[User],
      // mentionRoles: Vector[Role],
      embeds: Option[Vector[Embed]] = None,
      pinned: Boolean,
      `type`: Int // TODO: convert message type to real human readable form
  ) extends EventObject derives RW

end EventData
