package com.github.foldcat.dislocated.client

import com.github.foldcat.dislocated.impl.client.apicall.*
import com.github.foldcat.dislocated.impl.client.httpthrottler.*
import com.github.foldcat.dislocated.impl.util.label.Label.*
import com.github.foldcat.dislocated.objects.EventData
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
  * ## edge case
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
  * #### see also
  *
  * priority queue sorting info:
  * [[package com.github.foldcat.dislocated.impl.client.apicall]]
  *
  * throttler for into on throttling:
  * [[package com.github.foldcat.dislocated.impl.client.httpthrottler]]
  *
  * per bucket based throttling:
  * [[package com.github.foldcat.dislocated.impl.client.bucketexecutor]]
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

trait ClientCall[R, T, S](implicit
    client: Client[T],
    system: ActorSystem[S]
):
  var payload: Json                 = obj()
  implicit val ec: ExecutionContext = system.executionContext

// APPLICATION

class GetCurrentApplication[T, S](implicit
    client: Client[T],
    system: ActorSystem[S]
) extends ClientCall[Application, T, S]:

  def run: Future[Application] =
    val promise = Promise[Json]()
    val locator = s"${client.apiUrl}/application/@me"
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = GET,
        uri = locator
      ),
      promise,
      locator
    )
    promise.future.map(data => data.as[Application])

class EditCurrentApplication[T, S: ActorSystem](implicit
    client: Client[T]
) extends ClientCall[Application, T, S]:

  def customInstallUrl(s: String) =
    payload = payload.merge(
      obj("custom_install_url" -> s)
    )
    this

  def description(s: String) =
    payload = payload.merge(
      obj("description" -> s)
    )
    this

  def roleConnectionsVerificationUrl(s: String) =
    payload = payload.merge(
      obj("role_connections_verification_url" -> s)
    )
    this

  def installParams(i: InstallParams) =
    payload = payload.merge(i.json)
    this

  // def integrationTypesConfig()

  def flags(i: Int) =
    payload = payload.merge(
      obj("flags" -> i)
    )

  // def icon

  // def coverImage
  //
  def interactionEndpointUrl(s: String) =
    payload = payload.merge(
      obj("interactions_endpoint_url" -> s)
    )

  def tags(v: Vector[String]) =
    payload = payload.merge(
      obj("tags" -> v.asJson)
    )

  def run: Future[Application] =
    val promise = Promise[Json]()
    val locator = s"${client.apiUrl}/channels/@me"
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = PATCH,
        uri = locator
      ),
      promise,
      locator
    )
    promise.future.map(data => data.as[Application])

end EditCurrentApplication

// CHANNEL

class GetChannel[T, S: ActorSystem](channelID: String)(implicit
    client: Client[T]
) extends ClientCall[EventData.Channel, T, S]:
  def run: Future[EventData.Channel] =
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
    promise.future.map(data => data.as[EventData.Channel])

class ModifyChannel[T, S: ActorSystem](channelID: String)(implicit
    client: Client[T]
):
  def groupDM: ModifyGroupDM[T, S] =
    new ModifyGroupDM(channelID)
  def guildChannel =
    new ModifyGuildChannel(channelID)
  def thread =
    new ModifyThread(channelID)

class ModifyGroupDM[T, S: ActorSystem](channelID: String)(implicit
    client: Client[T]
) extends ClientCall[EventData.Channel, T, S]:

  def name(n: String) =
    payload = payload.merge(
      obj("name" -> n)
    )
    this

  // def icon() // base 64 encoded icon what

  def run: Future[EventData.Channel] =
    val promise = Promise[Json]()
    val locator = s"${client.apiUrl}/channels/$channelID"
    client.handler ! ApiCall.Call(
      HttpRequest(
        headers = List(client.authHeader),
        method = PATCH,
        uri = locator,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          payload.toString
        )
      ),
      promise,
      locator
    )
    promise.future.map(json => json.as[EventData.Channel])

end ModifyGroupDM

class ModifyGuildChannel[T](channelID: String)(implicit
    client: Client[T]
)

class ModifyThread[T](channelID: String)(implicit
    client: Client[T]
)

class CreateMessage[T](channelID: String)(implicit
    client: Client[T],
    context: ExecutionContext
):
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

end CreateMessage
