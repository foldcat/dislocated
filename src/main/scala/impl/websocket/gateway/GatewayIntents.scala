package org.maidagency.maidlib.impl.websocket.gateway

import scala.annotation.targetName

case class GatewayIntents(shift: Int, maybe: Option[Int] = Option.empty[Int]):
  private def value: Int = maybe.getOrElse(default = 1 << shift)
  def toInt: Int         = value
  def in(intent: GatewayIntents): Boolean = (this & intent) != 0
  def in(intent: Int): Boolean            = (this & intent) != 0
  @targetName("negate")
  private def unary_~ : Int = ~this.toInt
  @targetName("binary-or")
  def |(other: GatewayIntents): Int = this.toInt | other.toInt
  @targetName("binary-or-int")
  def |(other: Int): Int = this.toInt | other
  @targetName("binary-and")
  def &(other: GatewayIntents): Int = this.toInt & other.toInt
  @targetName("binary-and-int")
  def &(other: Int): Int                = this.toInt & other
  def unset(other: GatewayIntents): Int = this & ~other
  def unset(other: Int): Int            = this & ~other
end GatewayIntents

implicit class IntToIntent(val value: Int) extends AnyVal:
  def toIntent: GatewayIntents = GatewayIntents(0, Option(value))

object GatewayIntents:
  val NONE: GatewayIntents             = GatewayIntents(0, maybe = Option(0))
  val GUILDS: GatewayIntents           = GatewayIntents(0)
  val GUILD_MEMBERS: GatewayIntents    = GatewayIntents(1)
  val GUILD_MODERATION: GatewayIntents = GatewayIntents(2)
  val GUILD_EMOJIS_AND_STICKERS: GatewayIntents     = GatewayIntents(3)
  val GUILD_INTEGRATIONS: GatewayIntents            = GatewayIntents(4)
  val GUILD_WEBHOOKS: GatewayIntents                = GatewayIntents(5)
  val GUILD_INVITES: GatewayIntents                 = GatewayIntents(6)
  val GUILD_VOICE_STATES: GatewayIntents            = GatewayIntents(7)
  val GUILD_PRESENCES: GatewayIntents               = GatewayIntents(8)
  val GUILD_MESSAGES: GatewayIntents                = GatewayIntents(9)
  val GUILD_MESSAGE_REACTIONS: GatewayIntents       = GatewayIntents(10)
  val GUILD_MESSAGE_TYPING: GatewayIntents          = GatewayIntents(11)
  val DIRECT_MESSAGES: GatewayIntents               = GatewayIntents(12)
  val DIRECT_MESSAGE_REACTIONS: GatewayIntents      = GatewayIntents(13)
  val DIRECT_MESSAGE_TYPING: GatewayIntents         = GatewayIntents(14)
  val MESSAGE_CONTENT: GatewayIntents               = GatewayIntents(15)
  val GUILD_SCHEDULED_EVENTS: GatewayIntents        = GatewayIntents(16)
  val AUTO_MODERATION_CONFIGURATION: GatewayIntents = GatewayIntents(20)
  val AUTO_MODERATION_EXECUTION: GatewayIntents     = GatewayIntents(21)
  val GUILD_MESSAGE_POLLS: GatewayIntents           = GatewayIntents(24)
  val DIRECT_MESSAGE_POLLS: GatewayIntents          = GatewayIntents(25)
  val ALL: GatewayIntents = GatewayIntents(0, Option(0x331ffff))
