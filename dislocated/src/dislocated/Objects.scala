package com.github.foldcat.dislocated.objects

import fabric.*
import fabric.rw.*

/*  Partial + Usable Response Representation
 *  Partial: Don't expect more data than absolutely required
 *  Usable: TODO We add more data optionally if it makes life easier for the end user
 *  Response: This is the data on the receiving end, coming from Discord
 *  Representation: Define a format that can be parsed from JSON but is more type-safe
 */

// you know what, this part is pure manual labour
// sit down and enjoy the fruit of my heardwork
// oh my goodness i hate entering all these

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
      derives RW

  // APPLICATION

  case class Application(
      id: String,
      name: String,
      icon: Option[String],
      description: String,
      rpcOrigins: Option[Vector[String]],
      botPublic: Boolean,
      botRequireCodeGrant: Boolean,
      bot: Option[User],
      termsOfServiceUrl: Option[String],
      privacyPolicyUrl: Option[String],
      owner: Option[User],
      summary: String, // deprecated
      verifyKey: String,
      team: Option[Team],
      guildId: Option[String],
      guild: Option[Guild],
      primarySkuId: Option[String],
      slug: Option[String],
      coverImage: Option[String],
      flags: Int,
      approximateGuildCount: Int,
      redirectUris: Option[Vector[String]],
      interactionsEndpointUrl: Option[String],
      roleConnectionsVerificationUrl: Option[String],
      tags: Option[Vector[String]],
      installParams: Option[InstallParams],
      // integrationTypesConfig: Option[
      //   Map[String, IntegrationTypeConfiguration]
      // ],
      customInstallUrl: Option[String]
  ) extends EventObject
      derives RW

  case class InstallParams(
      scopes: Vector[String],
      permissions: Vector[String]
  ) extends EventObject
      derives RW

  case class Team(
      icon: Option[String],
      id: String,
      members: Vector[TeamMember],
      name: String,
      ownerUserId: String
  ) extends EventObject
      derives RW

  case class TeamMember(
      membershipState: Int,
      teamId: String,
      user: User,
      role: String
  ) extends EventObject
      derives RW

  case class ApplicationRoleConnectionMetadata(
      `type`: Int,
      key: String,
      name: String,
      nameLocalizations: Option[Map[String, String]],
      description: String,
      descriptionLocalizations: Option[Map[String, String]]
  ) extends EventObject
      derives RW

  case class AuditLog(
      applicationCommands: Vector[ApplicationCommand],
      auditLogEntries: Vector[AuditLogEntry],
      autoModerationRules: Vector[AutoModerationRule],
      guildScheduledEvents: Vector[GuildScheduledEvent],
      // integrations: Vector[PartialIntegration],
      // threads: Vector[ThreadChannel],
      users: Vector[User]
      // webhooks: Vector[Webhook]
  ) extends EventObject
      derives RW

  case class GuildScheduledEvent(
      id: String,
      guildId: String,
      channelId: Option[String],
      creatorId: Option[String],
      name: String,
      description: Option[String],
      scheduledStartTime: String,
      scheduledEndTime: Option[String],
      privacyLevel: Int,
      status: Int,
      entityType: Int,
      entityId: Option[String],
      entityMetadata: Option[EntityMetadata],
      creator: Option[User],
      userCount: Option[Int],
      image: Option[String]
  ) extends EventObject
      derives RW

  case class EntityMetadata(
      location: Option[String]
  ) extends EventObject
      derives RW

  case class AuditLogEntry(
      targetId: Option[String],
      changes: Option[Vector[AuditLogChange]],
      userId: Option[String],
      id: String,
      actionType: Int,
      options: Option[AuditLogEntryInfo],
      reason: Option[String]
  ) extends EventObject
      derives RW

  case class AuditLogChange(
      // newValue: Option[Any], // TODO: typesafe
      // oldValue: Option[Any],
      key: String
  ) extends EventObject
      derives RW

  case class AutoModerationRule(
      id: String,
      guildId: String,
      name: String,
      creatorId: String,
      eventType: Int,
      triggerType: Int,
      // triggerMetadata: Any,
      actions: Vector[Action],
      enabled: Boolean,
      exemptRoles: Vector[String],
      exemptChannels: Vector[String]
  ) extends EventObject
      derives RW

  case class Action(
      `type`: Int,
      metadata: Option[ActionMetadata]
  ) extends EventObject
      derives RW

  case class ActionMetadata(
      channelId: Option[String],
      durationSeconds: Option[Int],
      customMessage: Option[String]
  ) extends EventObject
      derives RW

  case class AuditLogEntryInfo(
      applicationId: Option[String],
      autoModerationRuleName: Option[String],
      autoModerationRuleTriggerType: Option[String],
      channelId: Option[String],
      count: Option[String],
      deleteMemberDays: Option[String],
      id: Option[String],
      membersRemoved: Option[String],
      messageId: Option[String],
      roleName: Option[String],
      `type`: Option[String],
      integrationType: Option[String]
  ) extends EventObject
      derives RW

  case class ApplicationCommand(
      id: String,
      `type`: Option[Int],
      applicationId: String,
      guildId: Option[String],
      name: String,
      nameLocalizations: Option[Map[String, String]],
      description: String,
      descriptionLocalizations: Option[Map[String, String]],
      options: Option[Vector[ApplicationCommandOption]],
      defaultMemberPermissions: Option[String],
      dmPermission: Option[Boolean],
      defaultPermission: Option[Boolean],
      nsfw: Boolean,
      // integrationTypes: Option[Vector[IntegrationType]],
      // contexts: Option[Vector[InteractionContextType]],
      version: String
  ) extends EventObject
      derives RW

  case class ApplicationCommandOption(
      // `type`: ApplicationCommandType,
      name: String,
      nameLocalizations: Option[Map[String, String]],
      description: String,
      descriptionLocalizations: Option[Map[String, String]],
      required: Boolean,
      // choices: Option[Vector[ApplicationCommandOptionChoice]],
      options: Option[Vector[ApplicationCommandOption]],
      // channelTypes: Option[Vector[ChannelType]],
      minValue: Option[Int],
      maxValue: Option[Double],
      minLength: Option[Int],
      maxLength: Option[Int],
      autocomplete: Option[Boolean]
  ) extends EventObject
      derives RW

  case class GuildMember(
      // roles: Option[Vector[String]] = None,
      joinedAt: String,
      deaf: Boolean,
      mute: Boolean,
      flags: Int
  ) extends EventObject
      derives RW

  case class User(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None
  ) extends EventObject
      derives RW

  case class UserWithMember(
      id: String,
      username: String,
      discriminator: String,
      globalName: Option[String] = None,
      avatar: Option[String] = None,
      member: GuildMember
  ) extends EventObject
      derives RW

  case class Member(
      user: Option[User] = None,
      nick: Option[String] = None,
      avatar: Option[String] = None,
      roles: Vector[String],
      joinedAt: String,
      premiumSince: Option[String] = None,
      deaf: Boolean,
      mute: Boolean,
      flags: Int,
      pending: Option[Boolean] = None,
      permissions: Option[String] = None,
      communicationDisabledUntil: Option[String] = None,
      avatarDecorationData: Option[AvatarDecorationData] = None
  ) extends EventObject
      derives RW

  case class Guild(
      id: String,
      name: String,
      icon: Option[String] = None,
      iconHash: Option[String] = None,
      splash: Option[String] = None,
      discoverySplash: Option[String] = None,
      owner: Option[Boolean] = None,
      ownerId: String,
      permissions: Option[String] = None,
      region: Option[String] = None,
      afkChannelId: Option[String] = None,
      afkTimeout: Int,
      widgetEnabled: Boolean,
      widgetChannelId: Option[String] = None,
      verificationLevel: Int,
      defaultMessageNotifications: Int,
      explicitContentFilter: Int,
      // roles: Vector[Role],
      emojis: Vector[Emoji],
      features: Vector[String],
      mfaLevel: Int,
      applicationId: Option[String] = None,
      systemChannelId: Option[String] = None,
      systemChannelFlags: Int,
      rulesChannelId: Option[String] = None,
      maxPresences: Option[Int] = None,
      maxMembers: Int,
      vanityUrlCode: Option[String] = None,
      description: Option[String] = None,
      banner: Option[String] = None,
      premiumTier: Int,
      premiumSubscriptionCount: Option[Int] = None,
      preferredLocale: String,
      publicUpdatesChannelId: Option[String] = None,
      maxVideoChannelUsers: Int,
      maxStageVideoChannelUsers: Int,
      approximateMemberCount: Option[Int] = None,
      approximatePresenceCount: Option[Int] = None,
      welcomeScreen: Option[WelcomeScreen] = None,
      nsfwLevel: Int,
      // stickers: Option[Vector[Sticker]] = None,
      premiumProgressBarEnabled: Boolean,
      safetyAlertsChannelId: Option[String] = None
  ) extends EventObject
      derives RW

  case class WelcomeScreen(
      description: Option[String],
      welcomeChannels: Vector[WelcomeScreenChannel]
  ) extends EventObject
      derives RW

  case class WelcomeScreenChannel(
      channelId: String,
      description: String,
      emojiId: Option[String],
      emojiName: Option[String]
  ) extends EventObject
      derives RW

  case class Emoji(
      id: Option[String] = None,
      name: Option[String] = None,
      roles: Option[Vector[String]] = None,
      user: Option[User] = None,
      requireColons: Boolean,
      managed: Boolean,
      animated: Boolean,
      available: Boolean
  ) extends EventObject
      derives RW

  case class AvatarDecorationData(
      asset: String,
      skuId: String
  ) extends EventObject
      derives RW

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
  ) extends EventObject
      derives RW

  case class Reaction() extends EventObject

  case class MessageActivity() extends EventObject

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
  ) extends EventObject
      derives RW

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
  ) extends EventObject
      derives RW

end EventData
