package org.maidagency.maidlib.objects

import upickle.default.*
import java.sql.Timestamp

sealed trait Events derives ReadWriter

case class MessageCreateEvent(
    id: String,
    channel_id: String,
    author: User,
    content: Option[String] = None,
    timestamp: String,
    edited_timestamp: Option[String] = None,
    tts: Boolean,
    mentionEveryone: Boolean,
    mentions: Vector[User],
    mentionRoles: Vector[Role],
    mentionChannels: Option[Vector[ChannelMention]] = None,
    embeds: Option[Embed] = None,
    reactions: Option[Vector[Reaction]] = None,
    nonce: Option[String | Int] = None,
    pinned: Boolean,
    webhookId: Option[String] = None,
    @upickle.implicits.key("type")
    messageType: Integer, // TODO: convert message type to real human readable form
    activity: Option[MessageActivity],
    application: Option[Application] = None,
    applicationID: Option[String] = None,
    messageReference: Option[MessageReference] = None,
    flags: Option[Int] = None,
    referencedMessage: Option[Message] = None,
    interactionMetadata: Option[MessageInteractionMetadata] = None,
    interaction: Option[MessageInteraction] = None,
    thread: Option[Channel] = None,
    components: Option[Vector[Int]] =
      None, // TODO: figure out what is this thing
    stickerItems: Option[StickerItem] = None,
    stickers: Option[Vector[Sticker]] = None,
    position: Option[Int] = None,
    roleSubscriptionData: Option[RoleSubscriptionData] = None,
    resolved: Option[ResolvedData],
    poll: Option[Poll],
    guild_id: String = None,
    member: Option[GuildMember] = None,
    mentions: Vector[UserWithMember]
) extends Events

case class GuildMember(
    user: Option[User] = None,
    nick: Option[String] = None,
    avatar: Option[String] = None,
    roles: Vector[String],
    joined_at: ISO8601 Timestamp,
    premium_since: Option[ISO8601 Timestamp] = None,
    deaf: Boolean,
    mute: Boolean,
    flags: Int,
    pending: Option[Boolean] = None,
    permissions: Option[String] = None,
    communication_disabled_until: Option[ISO8601 Timestamp]
)

case class User(
    id: Int,
    username: String,
    discriminator: String,
    globalName: String,
    avatar: String,
    bot: Option[String],
    system: Option[String],
    mfaEnabled: Option[String],
    banner: Option[String],
    accentColor: Option[Int],
    locale: Option[String],
    verified: Option[Boolean],
    email: Option[String],
    flags: Option[Int],
    premiumType: Option[Int],
    avatarDecoration: Option[String]
)

case class Role()

case class ChannelMention()

case class Embed()

case class Reaction()

case class MessageActivity()

case class Application()

case class MessageReference()

case class MessageInteractionMetadata()

case class MessageInteraction()

case class Channel()

case class StickerItem()

case class Sticker()

case class RoleSubscriptionData()

case class ResolvedData()

case class Poll()

case class Message(
    id: String,
    channel_id: String,
    author: User,
    content: Option[String] = None,
    timestamp: String,
    edited_timestamp: Option[String] = None,
    tts: Boolean,
    mentionEveryone: Boolean,
    mentions: Vector[User],
    mentionRoles: Vector[Role],
    mentionChannels: Option[Vector[ChannelMention]] = None,
    embeds: Option[Embed] = None,
    reactions: Option[Vector[Reaction]] = None,
    nonce: Option[String | Int] = None,
    pinned: Boolean,
    webhookId: Option[String] = None,
    @upickle.implicits.key("type")
    messageType: Integer, // TODO: convert message type to real human readable form
    activity: Option[MessageActivity],
    application: Option[Application] = None,
    applicationID: Option[String] = None,
    messageReference: Option[MessageReference] = None,
    flags: Option[Int] = None,
    referencedMessage: Option[Message] = None,
    interactionMetadata: Option[MessageInteractionMetadata] = None,
    interaction: Option[MessageInteraction] = None,
    thread: Option[Channel] = None,
    components: Option[Vector[Int]] =
      None, // TODO: figure out what is this thing
    stickerItems: Option[StickerItem] = None,
    stickers: Option[Vector[Sticker]] = None,
    position: Option[Int] = None,
    roleSubscriptionData: Option[RoleSubscriptionData] = None,
    resolved: Option[ResolvedData],
    poll: Option[Poll]
)
