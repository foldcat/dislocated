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
  ) extends Events
  object MessageCreateEvent:
    implicit val rw: RW[MessageCreateEvent] =
      RW.gen[MessageCreateEvent]

  case class GuildMember(
      roles: Seq[String],
      joinedAt: String,
      deaf: Boolean,
      mute: Boolean,
      flags: Int
  ) extends EventObject

  case class User(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None
  ) extends EventObject
  object User:
    implicit val rw: RW[User] = RW.gen[User]

  case class UserWithMember(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None,
      member: GuildMember
  ) extends EventObject

  case class Member() extends EventObject

  case class Role() extends EventObject

  case class ChannelMention() extends EventObject

  case class Embed() extends EventObject

  case class Reaction() extends EventObject

  case class MessageActivity() extends EventObject

  case class Application() extends EventObject

  case class MessageReference() extends EventObject

  case class MessageInteractionMetadata() extends EventObject

  case class MessageInteraction() extends EventObject

  case class Channel(
      id: String,
      `type`: Int
  ) extends EventObject

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
      embeds: Option[Embed] = None,
      pinned: Boolean,
      `type`: Int // TODO: convert message type to real human readable form
  ) extends EventObject

end EventData
