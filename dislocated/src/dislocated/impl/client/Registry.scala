package com.github.foldcat.dislocated.impl.client.registry

import com.github.foldcat.dislocated.impl.client.apicall.ApiCall.*
import com.github.foldcat.dislocated.impl.client.bucketrouter.*
import java.util.concurrent.*
import java.util.function.*
import org.apache.pekko
import pekko.actor.typed.*

/** registry stores which bucket corresponds to what uri in order to
  * avoid rate limits
  *
  * routes request to its correct bucket
  */
class Registry:

  extension [K, V](cm: ConcurrentHashMap[K, V])
    /** sget wraps .get in Option
      *
      * @param key
      *   the value to be .get-ed
      * @return
      *   return of .get wrapped in Option
      */
    def sget(key: K) =
      Option(cm.get(key))

  // key: bucket
  // val: actorRef, may not exist
  val bucketStore =
    ConcurrentHashMap[String, ActorRef[Call]]()

  // key: uri
  // val: bucket
  val uriStore = ConcurrentHashMap[String, String]()

  /** query the ActorRef request is supposed to be routed to
    *
    * @param uri
    *   uri of said request
    * @return
    *   Option with an ActorRef, if None, please route the data to the
    *   default executor
    */
  def route(uri: String): Option[ActorRef[Call]] =
    uriStore
      .sget(uri)
      .flatMap(res => bucketStore.sget(res))

  /** shall be called upon success api call
    *
    * updates the registry with latest.trace
    *
    * @param uri
    *   uri of said request
    * @param bucket
    *   bucket of said request
    * @return
    *   Boolean, if true, one should call registerActor as no actor is
    *   associated by said bucket
    */
  def update(uri: String, bucket: String) =
    uriStore.put(
      uri,
      bucket
    )
    bucketStore.sget(bucket) == None

  /** associate bucket with ActorRef
    *
    * asserts the bucket doesn't exist
    *
    * @param bucket
    *   bucket to be associated
    * @param ref
    *   ActorRef to be stored
    * @return
    *   don't have to care about the return
    */
  def registerActor(bucket: String, ref: ActorRef[Call]) =
    assert(bucketStore.sget(bucket) == None) // TODO: custom error
    bucketStore.put(bucket, ref)

end Registry
