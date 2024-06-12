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
