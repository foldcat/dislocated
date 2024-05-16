package org.maidagency.maidlib.impl.websocket.gateway

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.annotation.targetName
import scala.collection.mutable

class GatewayIntent(intent: Either[(Int, String), Int]):

  val logger = LoggerFactory.getLogger(classOf[GatewayIntent])

  private def value: Int = this.intent match
    case Left(shift, _) => 1 << shift
    case Right(bits)    => bits

  def toInt: Int = value

  override def toString: String = s"GatewayIntent[flags=${
      if this == GatewayIntent.ALL then "ALL"
      else
        GatewayIntent.INTENTS
          .map((k, v) => if k.in(this) then s"$v" else "")
          .filterNot(_.isEmpty)
          .reduceLeftOption((l, r) => s"$l, $r")
          .getOrElse("NONE")
    }]"

  def in(intent: GatewayIntent): Boolean = (this & intent) == this

  def in(intent: Int): Boolean = (this & intent) == this

  @targetName("negate")
  private def unary_~ : GatewayIntent = (~this.toInt).toIntent

  @targetName("binary-or")
  def |(other: GatewayIntent): GatewayIntent = this | other.toInt

  @targetName("binary-or-int")
  def |(other: Int): GatewayIntent = (this.toInt | other).toIntent

  @targetName("binary-and")
  def &(other: GatewayIntent): GatewayIntent = this & other.toInt

  @targetName("binary-and-int")
  def &(other: Int): GatewayIntent = (this.toInt & other).toIntent

  @targetName("xor")
  def ^(other: GatewayIntent): GatewayIntent = this ^ other.toInt

  @targetName("xor-int")
  def ^(other: Int): GatewayIntent = (this.toInt ^ other).toIntent

  @targetName("minus")
  def -(other: GatewayIntent): GatewayIntent = this & ~other

  @targetName("minus-int")
  def -(other: Int): GatewayIntent = this & ~other

  @targetName("plus")
  def +(other: GatewayIntent): GatewayIntent = this | other

  @targetName("plus-int")
  def +(other: Int): GatewayIntent = this | other

  @targetName("not-equal")
  def !=(other: GatewayIntent): Boolean = this != other.toInt

  @targetName("not-equal-int")
  def !=(other: Int): Boolean = this.toInt != other

  @targetName("equal")
  def ==(other: GatewayIntent): Boolean = this == other.toInt

  @targetName("equal-int")
  def ==(other: Int): Boolean = this.toInt == other

  this.intent match
    case Left(_, name) =>
      GatewayIntent.INTENTS.put(this.value, name)
      logger.debug(s"Saved flag: $this")
      logger.debug(
        s"The union of all flags is: ${GatewayIntent.INTENTS.keys.fold(0)((l, r) => l | r).toHexString}"
      )
    case _ => ()
end GatewayIntent

implicit class IntToIntent(val value: Int) extends AnyVal:
  def toIntent: GatewayIntent = GatewayIntent(Right(value))

  def in(other: GatewayIntent): Boolean = value.toIntent.in(other)

implicit class SetToIntent(val value: Set[GatewayIntent]) extends AnyVal:
  def toIntent: GatewayIntent = value
    .reduceOption((l, r) => l | r)
    .getOrElse(GatewayIntent.NONE)

object GatewayIntent:
  private val INTENTS: mutable.HashMap[Int, String] = new mutable.HashMap()
  /* meta intents */
  val ALL: GatewayIntent  = GatewayIntent(Right(0x331ffff))
  val NONE: GatewayIntent = GatewayIntent(Right(0))
  /* individual intents */
  val GUILDS: GatewayIntent =
    GatewayIntent(Left(0, "GUILDS"))
  val GUILD_MEMBERS: GatewayIntent =
    GatewayIntent(Left(1, "GUILD_MEMBERS"))
  val GUILD_MODERATION: GatewayIntent =
    GatewayIntent(Left(2, "GUILD_MODERATION"))
  val GUILD_EMOJIS_AND_STICKERS: GatewayIntent =
    GatewayIntent(Left(3, "GUILD_EMOJIS_AND_STICKERS"))
  val GUILD_INTEGRATIONS: GatewayIntent =
    GatewayIntent(Left(4, "GUILD_INTEGRATIONS"))
  val GUILD_WEBHOOKS: GatewayIntent =
    GatewayIntent(Left(5, "GUILD_WEBHOOKS"))
  val GUILD_INVITES: GatewayIntent =
    GatewayIntent(Left(6, "GUILD_INVITES"))
  val GUILD_VOICE_STATES: GatewayIntent =
    GatewayIntent(Left(7, "GUILD_VOICE_STATES"))
  val GUILD_PRESENCES: GatewayIntent =
    GatewayIntent(Left(8, "GUILD_PRESENCES"))
  val GUILD_MESSAGES: GatewayIntent =
    GatewayIntent(Left(9, "GUILD_MESSAGES"))
  val GUILD_MESSAGE_REACTIONS: GatewayIntent =
    GatewayIntent(Left(10, "GUILD_MESSAGE_REACTIONS"))
  val GUILD_MESSAGE_TYPING: GatewayIntent =
    GatewayIntent(Left(11, "GUILD_MESSAGE_TYPING"))
  val DIRECT_MESSAGES: GatewayIntent =
    GatewayIntent(Left(12, "DIRECT_MESSAGES"))
  val DIRECT_MESSAGE_REACTIONS: GatewayIntent =
    GatewayIntent(Left(13, "DIRECT_MESSAGE_REACTIONS"))
  val DIRECT_MESSAGE_TYPING: GatewayIntent =
    GatewayIntent(Left(14, "DIRECT_MESSAGE_TYPING"))
  val MESSAGE_CONTENT: GatewayIntent =
    GatewayIntent(Left(15, "MESSAGE_CONTENT"))
  val GUILD_SCHEDULED_EVENTS: GatewayIntent =
    GatewayIntent(Left(16, "GUILD_SCHEDULED_EVENTS"))
  val AUTO_MODERATION_CONFIGURATION: GatewayIntent =
    GatewayIntent(Left(20, "AUTO_MODERATION_CONFIGURATION"))
  val AUTO_MODERATION_EXECUTION: GatewayIntent =
    GatewayIntent(Left(21, "AUTO_MODERATION_EXECUTION"))
  val GUILD_MESSAGE_POLLS: GatewayIntent =
    GatewayIntent(Left(24, "GUILD_MESSAGE_POLLS"))
  val DIRECT_MESSAGE_POLLS: GatewayIntent =
    GatewayIntent(Left(25, "DIRECT_MESSAGE_POLLS"))
end GatewayIntent
