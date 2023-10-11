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

import java.io.{ByteArrayInputStream, IOException}

import io.opentracing.Span
import io.opentracing.mock.MockTracer
import io.opentracing.noop.NoopSpan
import io.opentracing.util.GlobalTracer
import org.apache.http._
import org.apache.http.client.ResponseHandler
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, HttpUriRequest, RequestBuilder}
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.openrepose.core.services.httpclient
import org.openrepose.core.services.httpclient.CachingHttpClientTest._
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.MDC

import scala.concurrent.duration._
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class CachingHttpClientTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  final val tracer = new MockTracer()

  var httpClient: CloseableHttpClient = _
  var cachingHttpClient: httpclient.CachingHttpClient = _

  GlobalTracer.register(tracer)

  override def beforeEach(): Unit = {
    super.beforeEach()

    MDC.clear()
    tracer.reset()
    httpClient = mock[CloseableHttpClient]

    when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
      .thenReturn(getBasicResponse)

    cachingHttpClient = new CachingHttpClient(httpClient, 1.day)
  }

  override def afterEach(): Unit = {
    super.afterEach()

    cachingHttpClient.close()
  }

  describe("close") {
    it("should delegate to the wrapped client") {
      cachingHttpClient.close()

      verify(httpClient).close()
    }
  }

  describe("getParams") {
    it("should delegate to the wrapped client") {
      cachingHttpClient.getParams

      verify(httpClient).getParams
    }
  }

  describe("getConnectionManager") {
    it("should delegate to the wrapped client") {
      cachingHttpClient.getConnectionManager

      verify(httpClient).getConnectionManager
    }
  }

  describe("doExecute") {
    Map(
      "(request)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(request)),
      "(request, context)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(request, context)),
      "(host, request)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(host, request)),
      "(host, request, context)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(host, request, context)),
      "(request, responseHandler)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(request, responseHandler)),
      "(request, responseHandler, context)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(request, responseHandler, context)),
      "(host, request, responseHandler)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(host, request, responseHandler)),
      "(host, request, responseHandler, context)" ->
        ((host: HttpHost, request: HttpUriRequest, responseHandler: ResponseHandler[CloseableHttpResponse], context: HttpContext) => cachingHttpClient.execute(host, request, responseHandler, context))
    ).foreach { case (description, executeFunction) =>
      it(s"should delegate a request to the underlying client using method signature $description") {
        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
          .thenReturn(mock[CloseableHttpResponse])

        executeFunction(
          HttpHost.create("localhost"),
          RequestBuilder.get("localhost").build(),
          new ResponseHandler[CloseableHttpResponse] {
            override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
          },
          CachingHttpClientContext.create()
        )

        verify(httpClient).execute(
          any[HttpHost],
          any[HttpRequest],
          any[HttpContext]
        )
      }
    }

    Map(
      "given only a key" ->
        (Some(Random.nextString(10)), None, None),
      "given a key and told not to force refresh the cache" ->
        (Some(Random.nextString(10)), None, Some(false)),
      "given a key and told to use the cache" ->
        (Some(Random.nextString(10)), Some(true), None),
      "given a key, told to use the cache, and told not to force refresh the cache" ->
        (Some(Random.nextString(10)), Some(true), Some(false))
    ).foreach { case (description, (cacheKey, useCache, forceRefresh)) =>
      it(s"should return a cached response when $description") {
        val request = RequestBuilder.get("localhost").build()
        val context = CachingHttpClientContext.create()

        cacheKey.foreach(context.setCacheKey)
        useCache.map(boolean2Boolean).foreach(context.setUseCache)
        forceRefresh.map(boolean2Boolean).foreach(context.setForceRefreshCache)

        cachingHttpClient.execute(request, context)
        cachingHttpClient.execute(request, context)

        verify(httpClient).execute(
          any[HttpHost],
          any[HttpRequest],
          any[HttpContext]
        )
      }
    }

    Map(
      "told to force refresh the cache" ->
        (Random.nextString(10), true),
      "told not to force refresh the cache" ->
        (Random.nextString(10), false)
    ).foreach { case (description, (cacheKey, forceRefresh)) =>
      it(s"should return a repeatable cached response when $description") {
        val request = RequestBuilder.get("localhost").build()
        val context = CachingHttpClientContext.create()

        context.setCacheKey(cacheKey)
        context.setUseCache(true)
        context.setForceRefreshCache(forceRefresh)

        val responseContent = "test content".getBytes
        val responseEntity = EntityBuilder.create()
          .setStream(new ByteArrayInputStream(responseContent))
          .build()
        val response = getBasicResponse
        response.setEntity(responseEntity)
        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
          .thenReturn(response)

        val freshResponse = cachingHttpClient.execute(request, context)
        val cachedResponse = cachingHttpClient.execute(request, context)

        responseEntity.isRepeatable shouldBe false
        freshResponse.getEntity.isRepeatable shouldBe true
        freshResponse.getEntity.getContentEncoding shouldEqual responseEntity.getContentEncoding
        freshResponse.getEntity.getContentType.getValue shouldEqual responseEntity.getContentType.getValue
        freshResponse.getEntity.getContentLength shouldEqual responseContent.length
        EntityUtils.toByteArray(freshResponse.getEntity) shouldEqual responseContent
        cachedResponse.getEntity.isRepeatable shouldBe true
        cachedResponse.getEntity.getContentEncoding shouldEqual responseEntity.getContentEncoding
        cachedResponse.getEntity.getContentType.getValue shouldEqual responseEntity.getContentType.getValue
        cachedResponse.getEntity.getContentLength shouldEqual responseContent.length
        EntityUtils.toByteArray(cachedResponse.getEntity) shouldEqual responseContent
        verify(httpClient, times(if (forceRefresh) 2 else 1)).execute(
          any[HttpHost],
          any[HttpRequest],
          any[HttpContext]
        )
      }
    }

    Map(
      "given nothing" ->
        (None, None, None),
      "not given a key and told not to force refresh the cache" ->
        (None, None, Some(false)),
      "not given a key and told to force refresh the cache" ->
        (None, None, Some(true)),
      "not given a key and told not to use the cache" ->
        (None, Some(false), None),
      "not given a key and told to use the cache" ->
        (None, Some(true), None),
      "not given a key and told not to use the cache and not to force refresh the cache" ->
        (None, Some(false), Some(false)),
      "not given a key and told not to use the cache and to force refresh the cache" ->
        (None, Some(false), Some(true)),
      "not given a key and told to use the cache and not to force refresh the cache" ->
        (None, Some(true), Some(false)),
      "not given a key and told to use the cache and to force refresh the cache" ->
        (None, Some(true), Some(true)),
      "given a key and told to force refresh the cache" ->
        (Some(Random.nextString(10)), None, Some(true)),
      "given a key and told not to use the cache" ->
        (Some(Random.nextString(10)), Some(false), None),
      "given a key and told not to use the cache and not to force refresh the cache" ->
        (Some(Random.nextString(10)), Some(false), Some(false)),
      "given a key and told not to use the cache and to force refresh the cache" ->
        (Some(Random.nextString(10)), Some(false), Some(true)),
      "given a key and told to use the cache and to force refresh the cache" ->
        (Some(Random.nextString(10)), Some(true), Some(true))
    ).foreach { case (description, (cacheKey, useCache, forceRefresh)) =>
      it(s"should not return a cached response when $description") {
        val request = RequestBuilder.get("localhost").build()
        val context = CachingHttpClientContext.create()

        cacheKey.foreach(context.setCacheKey)
        useCache.map(boolean2Boolean).foreach(context.setUseCache)
        forceRefresh.map(boolean2Boolean).foreach(context.setForceRefreshCache)

        cachingHttpClient.execute(request, context)
        cachingHttpClient.execute(request, context)

        verify(httpClient, times(2)).execute(
          any[HttpHost],
          any[HttpRequest],
          any[HttpContext]
        )
      }
    }

    it("should not return a cached response when the cache duration is 0") {
      cachingHttpClient = new httpclient.CachingHttpClient(httpClient, Duration.Zero)

      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()

      context.setCacheKey("testKey")

      cachingHttpClient.execute(request, context)
      cachingHttpClient.execute(request, context)

      verify(httpClient, times(2)).execute(
        any[HttpHost],
        any[HttpRequest],
        any[HttpContext]
      )
    }

    it(s"should not return a cached response when different cache keys are used") {
      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()
      val context2 = CachingHttpClientContext.create()
      context.setCacheKey("key1")
      context2.setCacheKey("key2")

      cachingHttpClient.execute(request, context)
      cachingHttpClient.execute(request, context2)

      verify(httpClient, times(2)).execute(
        any[HttpHost],
        any[HttpRequest],
        any[HttpContext]
      )
    }

    it(s"should update the cache when force refresh is used") {
      val mockResponse1 = mock[CloseableHttpResponse]
      val mockResponse2 = mock[CloseableHttpResponse]
      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
        .thenReturn(mockResponse1, mockResponse2)

      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()
      val context2 = CachingHttpClientContext.create()
      val context3 = CachingHttpClientContext.create()
      context.setCacheKey("key")
      context2.setCacheKey("key")
      context3.setCacheKey("key")
      context2.setForceRefreshCache(true)

      cachingHttpClient.execute(request, context)
      cachingHttpClient.execute(request, context2)
      val response3 = cachingHttpClient.execute(request, context3)

      verify(httpClient, times(2)).execute(
        any[HttpHost],
        any[HttpRequest],
        any[HttpContext]
      )
      response3 shouldBe mockResponse2
    }

    it("should rethrow an exception thrown by the underlying client") {
      val request = RequestBuilder.get("localhost").build()

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
        .thenThrow(new IOException())

      an[IOException] should be thrownBy cachingHttpClient.execute(request)
    }

    it("should carry over the active opentracing span when making a request") {
      val mockResponse = mock[CloseableHttpResponse]

      val localSpan = tracer.buildSpan("testSpan").start()
      tracer.scopeManager.activate(localSpan)

      var realSpan: Span = NoopSpan.INSTANCE
      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
        .thenAnswer((_: InvocationOnMock) => {
          realSpan = GlobalTracer.get.activeSpan()
          mockResponse
        })

      val request = RequestBuilder.get("localhost").build()

      cachingHttpClient.execute(request)

      realSpan shouldBe localSpan
    }

    it("should carry over the MDC when making a request") {
      val mockResponse = mock[CloseableHttpResponse]

      val localKey = "testKey"
      val localValue = "testValue"
      MDC.put(localKey, localValue)

      val remoteKey = "remoteKey"
      val remoteValue = "remoteValue"

      var realValue: String = "garbage"
      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext]))
        .thenAnswer((_: InvocationOnMock) => {
          realValue = MDC.get(localKey)
          MDC.put(remoteKey, remoteValue)
          mockResponse
        })

      val request = RequestBuilder.get("localhost").build()

      cachingHttpClient.execute(request)

      realValue shouldEqual localValue
      MDC.get(remoteKey) shouldBe remoteValue
    }
  }
}

object CachingHttpClientTest {
  def getBasicResponse: CloseableHttpResponse = {
    new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null) with CloseableHttpResponse {
      override def close(): Unit = {}
    }
  }
}
