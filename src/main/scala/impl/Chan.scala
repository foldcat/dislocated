package org.maidagency.maidlib.impl.chan

import org.apache.pekko
import org.maidagency.maidlib.impl.chan.Put.*
import org.slf4j.Logger
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
          throw new IllegalStateException(failedReason)
        case QueueClosed =>
          putLogger.debug("queue is closed")
          throw new IllegalAccessError("queue already closed")
