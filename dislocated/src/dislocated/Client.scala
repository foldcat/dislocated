package com.github.foldcat.dislocated.client

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.httpthrottler.*
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.objects.EventData.*
import fabric.*
import fabric.rw.*
import org.apache.pekko
import pekko.actor.typed.*
import pekko.actor.typed.scaladsl.*
import pekko.actor.typed.scaladsl.Behaviors
import pekko.http.scaladsl.*
import pekko.http.scaladsl.model.*
import pekko.http.scaladsl.model.headers.*
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import HttpMethods.*

/** an implicit class that must be provided for all the API calls
  *
  * it spawns up an actor, and all the API calls will be routed to
  * said actor in order to mitigate rate limit
  *
  * ## mechanism
  *
  * each Discord API calls are a Future obtained from a promise said
  * Promise is fed to the end user as a Future, and the Promise itself
  * will be fulfilled inside the actor spawned
  *
  * Discord API calls are fed to priority queues, and the sorting
  * behavior of said queue is defined by a pre-determined priority
  * (calls that triggers 429 too many requests takes highest priority,
  * other calls are less prioritized), and requests with the same
  * priority will be sorted based on timestamp instead
  *
  * we do not garentee orders queued up in parallel will result in
  * same pre-determined order of events sent to the API due to the
  * nature of a priority queue, however, the order should largely be
  * accurate enough
  *
  * we use a 1000 slots queue from pekko stream, failed to enqueue
  * error may be resulted by too much requests swarmed into said
  * queue, although I highly doubt the odds of this happening
  *
  * priority queues used are unbounded, way too much requests may
  * result in memory out of bounds error, although I highly doubt the
  * odds of this happening
  *
  * actors spawned for per bucket rate limit handling will self
  * terminate after certain amount of times when there are no messages
  * recieved and no calls are queued to clean up resources, see also
  * the httpthrottler package to see the percise timing, for this
  * behavior, there are an extremely slim edge case of call not being
  * handled, it is also safe to assume said case will never happen as
  * this is all only a theory
  *
  * @see
  *   priority queue sorting info
  *   [[package com.github.foldcat.dislocated.impl.client.apicall]]
  *
  * @see
  *   throttler for into on throttling
  *   [[package com.github.foldcat.dislocated.impl.client.httpthrottler]]
  *
  * @see
  *   per bucket based throttling
  *   [[package com.github.foldcat.dislocated.impl.client.bucketexecutor]]
  *
  * @param token
  *   Discord token
  * @param context
  *   actor context
  */
final class Client[T](
    token: String,
    context: ActorContext[T]
):
  private val versionNumber = 10

  val apiUrl = s"https://discord.com/api/v$versionNumber"

  val authHeader = RawHeader("Authorization", s"Bot $token")

  val handler = context.spawn( // TODO: allow user to self handle
    Behaviors
      .supervise(HttpThrottler())
      .onFailure[Exception]( // TODO: update catch
        SupervisorStrategy.restart
      ),
    genLabel("http-actor")
  )
end Client

// def submitRequest[P <: PURR, T](
//     req: HttpRequest,
//     target: PURRSum
// )(implicit client: Client[T]) =
//   val promise: Promise[P] = Promise[P]()
//   client.handler ! Call(
//     req,9
//     promise,
//     target
//   )
//   promise.future

// trait ApiCall:
//   def run: Future[Unit]

class GetChannel[T](channelID: String)(implicit client: Client[T]):
  def run: Future[Json] =
    val promise = Promise[Json]()
    val locator = s"${client.apiUrl}/channels/$channelID"
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = GET,
        uri = locator
      ),
      promise,
      locator
    )
    promise.future

class CreateMessage[T](channelID: String)(implicit client: Client[T]):

  var payload: Json = obj()

  def content(s: String) =
    payload = payload.merge(
      obj("content" -> s)
    )
    this

  def run: Future[Message] =
    val promise = Promise[Json]()
    val locator = s"${client.apiUrl}/channels/$channelID/messages"
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = POST,
        uri = locator,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          payload.toString
        )
      ),
      promise,
      locator
    )
    promise.future
      .map(json => json.as[Message])
