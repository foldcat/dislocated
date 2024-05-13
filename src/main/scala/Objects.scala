package org.maidagency.maidlib.objects

import java.sql.Timestamp
import upickle.default.{ReadWriter => RW, *}

sealed trait Events derives RW
/*  Partial + Usable Response Representation
 *  Partial: Don't expect more data than absolutely required
 *  Usable: TODO We add more data optionally if it makes life easier for the end user
 *  Response: This is the data on the receiving end, coming from Discord
 *  Representation: Define a format that can be parsed from JSON but is more type-safe
 */
sealed trait PURR derives RW

case class MessageCreateEvent(
    id: String,
    channelId: String,
    author: User,
    content: Option[String] = None,
    timestamp: String,
    editedTimestamp: Option[String] = None,
    tts: Boolean,
    mentionEveryone: Boolean,
    mentionRoles: Vector[Role],
    embeds: Option[Embed] = None,
    pinned: Boolean,
    @upickle.implicits.key("type")
    messageType: Integer, // TODO: convert message type to real human readable form
    guild_id: Option[String] = None,
    member: Option[GuildMember] = None,
    mentions: Vector[UserWithMember]
) extends Events

case class GuildMember(
    roles: Vector[String],
    joinedAt: String,
    deaf: Boolean,
    mute: Boolean,
    flags: Int
) extends PURR

case class User(
    id: String,
    username: String,
    discriminator: String,
    globalName: Option[String] = None,
    avatar: Option[String] = None
) extends PURR

case class UserWithMember(
    id: String,
    username: String,
    discriminator: String,
    globalName: Option[String] = None,
    avatar: Option[String] = None,
    member: GuildMember
) extends PURR

case class Member() extends PURR

case class Role() extends PURR

case class ChannelMention() extends PURR

case class Embed() extends PURR

case class Reaction() extends PURR

case class MessageActivity() extends PURR

case class Application() extends PURR

case class MessageReference() extends PURR

case class MessageInteractionMetadata() extends PURR

case class MessageInteraction() extends PURR

case class Channel() extends PURR

case class StickerItem() extends PURR

case class Sticker() extends PURR

case class RoleSubscriptionData() extends PURR

case class ResolvedData() extends PURR

case class Poll() extends PURR

case class Message(
    id: String,
    channelId: String,
    author: User,
    content: Option[String] = None,
    timestamp: String,
    editedTimestamp: Option[String] = None,
    tts: Boolean,
    mentionEveryone: Boolean,
    mentions: Vector[User],
    mentionRoles: Vector[Role],
    embeds: Option[Embed] = None,
    pinned: Boolean,
    @upickle.implicits.key("type")
    messageType: Int // TODO: convert message type to real human readable form
) extends PURR
