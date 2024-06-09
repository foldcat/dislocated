package com.github.foldcat.dislocated.impl.websocket.chan

import com.github.foldcat.dislocated.impl.util.customexception.*
import org.apache.pekko
import org.slf4j.LoggerFactory
import pekko.stream.BoundedSourceQueue
import pekko.stream.QueueOfferResult.*

class PutLog

object Put:

  final private def putLogger =
    LoggerFactory.getLogger(classOf[PutLog])

  // enqueue `in` into `q`
  // handles all the cases
  extension [T](q: BoundedSourceQueue[T])
    def !<(in: T): Unit =
      q.offer(in) match
        case Enqueued =>
          putLogger.debug(s"shoved in $in")
        case Dropped =>
          putLogger.debug(s"dropped $in")
        case Failure(cause) =>
          val failedReason = s"offer failed: ${cause.getMessage}"
          putLogger.debug(failedReason)
          throw new WebsocketFailure(failedReason)
        case QueueClosed =>
          putLogger.debug("queue is closed")
          throw new WebsocketFailure("queue already closed")
