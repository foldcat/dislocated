package org.maidagency.maidlib.objects

case class User(
    id: Int,
    username: String,
    discriminator: String,
    global_name: String,
    avatar: String,
    bot: Option[String],
    system: Option[String],
    mfa_enabled: Option[String],
    banner: Option[String],
    accent_color: Option[Int],
    locale: Option[String],
    verified: Option[Boolean],
    email: Option[String],
    flags: Option[Int],
    premium_type: Option[Int],
    avatar_decoration: Option[String]
)

case class Message(
    id: String,
    channel_id: String,
    author: User,
    content: Option[String],
    timestamp: String
)
