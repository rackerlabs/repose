/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.httpclient

import com.google.common.cache.{Cache, CacheBuilder}
import com.typesafe.scalalogging.StrictLogging
import org.apache.http._
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.params.HttpParams
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
  * Adds caching functionality to a [[CloseableHttpClient]].
  * This is accomplished by following the decorator pattern and delegating to a wrapped [[CloseableHttpClient]].
  *
  * The magnitude of the [[cacheDuration]] must be greater than or equal to zero.
  * If the magnitude of the [[cacheDuration]] is zero, the cache will not be populated.
  *
  * @param httpClient    the [[CloseableHttpClient]] to delegate non-caching responsibilities to
  * @param cacheDuration the [[Duration]] for which to cache a [[Promise]] of a [[CloseableHttpClient]]
  */
class CachingHttpClient(httpClient: CloseableHttpClient, cacheDuration: Duration)
  extends CloseableHttpClient with StrictLogging {

  private val futureResponseCache: Cache[String, Promise[CloseableHttpResponse]] = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheDuration.length, cacheDuration.unit)
    .build()

  override protected def doExecute(target: HttpHost, request: HttpRequest, context: HttpContext): CloseableHttpResponse = {
    // Read the cache configuration from the context and assign defaults to missing attributes
    val cachingContext = Option(context).map(CachingHttpClientContext.adapt).getOrElse(CachingHttpClientContext.create())
    val cacheKey = Option(cachingContext.getCacheKey)
    val useCache = Option(cachingContext.getUseCache).forall(Boolean2boolean)
    val forceRefresh = Option(cachingContext.getForceRefreshCache).exists(Boolean2boolean)

    // Create a new Promise of a response
    val newPromise = Promise[CloseableHttpResponse]()

    // Handle caching
    val responsePromise = cacheKey match {
      case Some(key) if forceRefresh =>
        // Do not check the cache, but do refresh the cache value
        logger.debug("Populating cache for request with key {} to {}", key, target)
        futureResponseCache.put(key, newPromise)
        newPromise
      case Some(key) if useCache =>
        // Check the cache and return the cached value if present, otherwise populate the value
        logger.debug("Checking cache for request with key {} to {}", key, target)
        futureResponseCache.get(key, () => {
          logger.debug("Cache miss, populating cache for request with key {} to {}", key, target)
          newPromise
        })
      case None if useCache || forceRefresh =>
        // Cache should be used, but no key was provided -- log the error and process the request
        logger.error("Cache key not provided, cache cannot be used for request to {}", target)
        newPromise
      case _ =>
        // No caching behavior desired
        logger.debug("Cache not being used for request to {}", target)
        newPromise
    }

    // If this promise is responsible for providing a response,
    // then execute the request, make the response repeatedly readable, and complete the Promise.
    // Any Exception thrown during execution will bubble up just as it would if the client were
    // called directly.
    if (responsePromise eq newPromise) {
      // NOTE: makeRepeatable need not be called if we are not caching the response.
      // NOTE: However, always making the response repeatable simplifies the code and
      // NOTE: at worst reads the body one additional time.
      newPromise.success(makeRepeatable(httpClient.execute(target, request, context)))
    }

    // Block until a response is ready, then return that response
    // We leave it to the wrapped client to timeout if the request has stalled
    // Doing so should be safe since the wrapped client would be responsible if we had not wrapped it,
    // and since the client has configured mechanisms for timing out (e.g., connection timeout and socket timeout)
    Await.result(responsePromise.future, Duration.Inf)
  }

  override def close(): Unit = {
    httpClient.close()
    futureResponseCache.cleanUp()
  }

  // Delegating methods
  override def getParams: HttpParams = {
    httpClient.getParams
  }

  override def getConnectionManager: ClientConnectionManager = {
    httpClient.getConnectionManager
  }

  /**
    * Make the HTTP response repeatable so that it can be safely cached and returned multiple times.
    * This method may mutate the provided HTTP response.
    */
  private def makeRepeatable(response: CloseableHttpResponse): CloseableHttpResponse = {
    Option(response.getEntity)
      .filterNot(_.isRepeatable)
      .foreach { responseEntity =>
        response.setEntity(EntityBuilder.create()
          .setBinary(EntityUtils.toByteArray(responseEntity))
          .setContentType(ContentType.get(responseEntity))
          .build())
      }
    response
  }
}
