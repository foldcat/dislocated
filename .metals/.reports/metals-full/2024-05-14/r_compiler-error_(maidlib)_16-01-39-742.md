file://<WORKSPACE>/src/main/scala/Objects.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 3.3.3
Classpath:
<WORKSPACE>/.bloop/maidlib/bloop-bsp-clients-classes/classes-Metals-h7njxvlURWyGRIidrMD26w== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.9.9/semanticdb-javac-0.9.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.3.3/scala3-library_3-3.3.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-actor-typed_3/1.0.2/pekko-actor-typed_3-1.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-stream_3/1.0.2/pekko-stream_3-1.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-http_3/1.0.1/pekko-http_3-1.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_3/3.3.0/upickle_3-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.12/scala-library-2.13.12.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-actor_3/1.0.2/pekko-actor_3-1.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-slf4j_3/1.0.2/pekko-slf4j_3-1.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-protobuf-v3_3/1.0.2/pekko-protobuf-v3_3-1.0.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/reactivestreams/reactive-streams/1.0.4/reactive-streams-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/typesafe/ssl-config-core_3/0.6.1/ssl-config-core_3-0.6.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-http-core_3/1.0.1/pekko-http-core_3-1.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_3/3.3.0/ujson_3-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_3/3.3.0/upack_3-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_3/3.3.0/upickle-implicits_3-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/typesafe/config/1.4.3/config-1.4.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/pekko/pekko-parsing_3/1.0.1/pekko-parsing_3-1.0.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/parboiled/parboiled_3/2.5.0/parboiled_3-2.5.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_3/3.3.0/upickle-core_3-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_3/1.1.0/geny_3-1.1.0.jar [exists ]
Options:
-deprecation -feature -unchecked -Xsemanticdb -sourceroot <WORKSPACE>


action parameters:
offset: 1107
uri: file://<WORKSPACE>/src/main/scala/Objects.scala
text:
```scala
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
  implicit@@
case object MessageCreateEvent extends Events

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

```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2607)
	dotty.tools.dotc.transform.SymUtils$.isInaccessibleChildOf(SymUtils.scala:318)
	dotty.tools.dotc.typer.Namer$Completer.register$1(Namer.scala:908)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChild$$anonfun$1(Namer.scala:920)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.Namer$Completer.registerIfChild(Namer.scala:920)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:815)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:187)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:189)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.ensureCompleted(SymDenotations.scala:393)
	dotty.tools.dotc.typer.Typer.retrieveSym(Typer.scala:2991)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3016)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:2669)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3038)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3042)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2812)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3083)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:44)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$adapted$1(TyperPhase.scala:50)
	scala.Function0.apply$mcV$sp(Function0.scala:42)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:440)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:50)
	dotty.tools.dotc.typer.TyperPhase.runOn$$anonfun$3(TyperPhase.scala:84)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:84)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:246)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1323)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:262)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:270)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:279)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:71)
	dotty.tools.dotc.Run.compileUnits(Run.scala:279)
	dotty.tools.dotc.Run.compileSources(Run.scala:194)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:165)
	scala.meta.internal.pc.MetalsDriver.run(MetalsDriver.scala:45)
	scala.meta.internal.pc.PcCollector.<init>(PcCollector.scala:44)
	scala.meta.internal.pc.PcDocumentHighlightProvider.<init>(PcDocumentHighlightProvider.scala:16)
	scala.meta.internal.pc.ScalaPresentationCompiler.documentHighlight$$anonfun$1(ScalaPresentationCompiler.scala:178)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner