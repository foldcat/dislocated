package org.maidagency.maidlib.objects

import org.maidagency.maidlib.impl.util.json.CustomPickle.*

sealed trait Events derives ReadWriter
/*  Partial + Usable Response Representation
 *  Partial: Don't expect more data than absolutely required
 *  Usable: TODO We add more data optionally if it makes life easier for the end user
 *  Response: This is the data on the receiving end, coming from Discord
 *  Representation: Define a format that can be parsed from JSON but is more type-safe
 */
sealed trait PURR

case class Debug(x: String) extends Events

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
) extends Events

case class GuildMember(
    roles: Seq[String],
    joinedAt: String,
    deaf: Boolean,
    mute: Boolean,
    flags: Int
) extends PURR
    derives ReadWriter

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
) extends PURR
    derives ReadWriter

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
