package com.github.foldcat.dislocated.objects

import com.github.foldcat.dislocated.impl.util.json.CustomPickle.*

/*  Partial + Usable Response Representation
 *  Partial: Don't expect more data than absolutely required
 *  Usable: TODO We add more data optionally if it makes life easier for the end user
 *  Response: This is the data on the receiving end, coming from Discord
 *  Representation: Define a format that can be parsed from JSON but is more type-safe
 */

object EventData:

  type Events = MessageCreateEvent | Unimplemented

  type EventData = (Events, ujson.Value)

  case class Unimplemented() derives ReadWriter

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
      @upickle.implicits.key(
        "type"
      ) eventType: Int, // TODO: convert message type to real human readable form
      guildId: Option[String] = None
      // member: Option[GuildMember] = None,
      // mentions: Seq[UserWithMember]
  ) derives ReadWriter

  case class GuildMember(
      roles: Seq[String],
      joinedAt: String,
      deaf: Boolean,
      mute: Boolean,
      flags: Int
  ) derives ReadWriter

  case class User(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None
  ) derives ReadWriter

  case class UserWithMember(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None,
      member: GuildMember
  ) derives ReadWriter

  case class Member() derives ReadWriter

  case class Role() derives ReadWriter

  case class ChannelMention() derives ReadWriter

  case class Embed() derives ReadWriter

  case class Reaction() derives ReadWriter

  case class MessageActivity() derives ReadWriter

  case class Application() derives ReadWriter

  case class MessageReference() derives ReadWriter

  case class MessageInteractionMetadata() derives ReadWriter

  case class MessageInteraction() derives ReadWriter

  case class Channel(
      id: String,
      @upickle.implicits.key("type") channelType: Int
  ) derives ReadWriter

  case class StickerItem() derives ReadWriter

  case class Sticker() derives ReadWriter

  case class RoleSubscriptionData() derives ReadWriter

  case class ResolvedData() derives ReadWriter

  case class Poll() derives ReadWriter

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
      @upickle.implicits.key("type")
      messageType: Int // TODO: convert message type to real human readable form
  ) derives ReadWriter

end EventData
