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
offset: 1147
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
object MessageCreateEvent: 
  implicit val rw: RW[@@]

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
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:94)
	scala.meta.internal.pc.SignatureHelpProvider$.$anonfun$1(SignatureHelpProvider.scala:48)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile(LinearSeq.scala:280)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile$(LinearSeq.scala:278)
	scala.collection.immutable.List.dropWhile(List.scala:79)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:398)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner