package org.maidagency.impl.gateway

import fabric.*
import fabric.rw.*

case class GatewayPayload(
    op: Int,
    d: Option[HelloPayload],
    s: Option[Int],
    t: Option[String]
)

object GatewayPayload:
  implicit val rw: RW[GatewayPayload] = RW.gen[GatewayPayload]

case class HelloPayload(heartbeat_interval: Int, _trace: Option[Json])

object HelloPayload:
  implicit val rw: RW[HelloPayload] = RW.gen[HelloPayload]
