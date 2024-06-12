package com.github.foldcat.dislocated.gatewayintents

import org.slf4j.LoggerFactory
import scala.annotation.targetName
import scala.collection.mutable

/** ==overview==
  *
  * Intents are binary values used when Identifying with the Discord
  * Gateway. Intents each have a set of related events. Unspecified
  * intents will cause your app to not receive any associated Gateway
  * events.
  *
  * ==example==
  * {{{
  * // how to use all intents
  * def allIntents: GatewayIntent = ALL
  * // how to use no intents
  * def noIntents: GatewayIntent = NONE
  * // how to name intents individually
  * def messageIntent: GatewayIntent = MESSAGE_CONTENT
  * // how to manipulate intents in combination
  * def allButMessage: GatewayIntent = allIntents - messageIntent
  * // how to convert a set of multiple GatewayIntent to a single Intent
  * def intentsFromSet: GatewayIntent = Set(GUILD_MESSAGES, DIRECT_MESSAGES).toIntent
  * // how to convert a set of multiple GatewayIntent values to a single Intent
  * def intentsFromInt: GatewayIntent = (2 + 4 + 16 + 32).toIntent
  * // how to convert a GatewayIntent for use by the Discord Gateway
  * def intentValue: Int = allButMessage.toInt
  * // how to check membership in a GatewayIntent
  * def checking: Boolean = messageIntent.in(allButMessage)
  * }}}
  *
  * @param intent
  *   The value represented by this intent. If provided with a (Int i,
  *   String s), the value will be 1 to the power of i and associated
  *   with the name s. If provided with an Int i directly, the value
  *   will be i instead.
  */
class GatewayIntent(intent: Either[(Int, String), Int]):

  val.traceger = LoggerFactory.getLogger(classOf[GatewayIntent])

  private def value: Int = this.intent match
    case Left(shift, _) => 1 << shift
    case Right(bits)    => bits

  /** Generates the integer value that Discord expects when
    * Identifying from this GatewayIntent
    * @return
    *   an Int that can be sent to the Discord Gateway directly
    */
  def toInt: Int = value

  /** @return
    *   the String representation of this GatewayIntent
    */
  override def toString: String = s"GatewayIntent[flags=${
      if this == GatewayIntent.ALL then "ALL"
      else
        GatewayIntent.INTENTS
          .map((k, v) => if k.in(this) then s"$v" else "")
          .filterNot(_.isEmpty)
          .reduceLeftOption((l, r) => s"$l, $r")
          .getOrElse("NONE")
    }]"

  /** Membership check for the current and provided intent: is the
    * provided intent included in the current intent?
    *
    * @param intent
    *   the intent to check membership for
    * @return
    *   Whether intent is in this GatewayIntent
    */
  def in(intent: GatewayIntent): Boolean = (this & intent) == this

  /** Membership check for the current intent and provided integer:
    * is(are) the intent(s) represented by the provided integer
    * included in the current intent?
    *
    * @param intent
    *   the integer representing intent(s) to check membership for
    * @return
    *   Whether intent is in this GatewayIntent
    */
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
     .traceger.debug(s"Saved flag: $this")
     .traceger.debug(
        s"The union of all flags is: ${GatewayIntent.INTENTS.keys
            .fold(0)((l, r) => l | r)
            .toHexString}"
      )
    case _ => ()
end GatewayIntent

/** Provides implicit conversion from an Int value to GatewayIntent
  * value
  *
  * @param value
  *   The original value
  * @return
  *   A GatewayIntent corresponding to the original value
  */
implicit class IntToIntent(private val value: Int) extends AnyVal:
  def toIntent: GatewayIntent = GatewayIntent(Right(value))

  def in(other: GatewayIntent): Boolean = value.toIntent.in(other)

/** Provides implicit conversion from a Set of GatewayIntent values to
  * a single GatewayIntent value
  *
  * @param value
  *   The original set of GatewayIntents
  * @return
  *   A GatewayIntent corresponding to the original set
  */
implicit class SetToIntent(private val value: Set[GatewayIntent])
    extends AnyVal:
  def toIntent: GatewayIntent = value
    .reduceOption((l, r) => l | r)
    .getOrElse(GatewayIntent.NONE)

/** Provides a set of GatewayIntent constant values which can be used
  * by name instead of number
  */
object GatewayIntent:
  private val INTENTS: mutable.HashMap[Int, String] =
    new mutable.HashMap()
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
