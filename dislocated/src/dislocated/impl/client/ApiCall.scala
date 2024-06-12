package com.github.foldcat.dislocated.impl.client.apicall

import fabric.*
import org.apache.pekko
import pekko.http.scaladsl.model.*
import scala.concurrent.*

/** ApiCall actor message shared between multiple actors
  */
enum ApiCall:
  case Call(
      effect: HttpRequest,
      promise: Promise[Json],
      uri: String
  )
  case QueueCall(uri: String, bucket: String)
  case Prior(call: QueuedExecution)
  case Terminate
  case SetInfo(bucket: Option[String], uri: String)

case class QueuedExecution(
    priority: Int,
    call: ApiCall.Call,
    timestamp: Int
)

object QueuedExecution:
  given cOrd: Ordering[QueuedExecution] with
    def compare(a: QueuedExecution, b: QueuedExecution) =
      if a.priority == b.priority then
        b.timestamp.compare(a.timestamp)
      else b.priority.compare(a.priority)
