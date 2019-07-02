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

import java.io.IOException
import java.util.UUID

import org.apache.http.client.methods.{CloseableHttpResponse, RequestBuilder}
import org.apache.http.client.{ClientProtocolException, ResponseHandler}
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHost, HttpRequest, HttpResponse}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, eq => isEq}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpClientServiceClientTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  val clientId: String = "test-client-id"

  var internalHttpClientService: InternalHttpClientService = _
  var internalHttpClient: InternalHttpClient = _
  var httpClient: CloseableHttpClient = _
  var httpResponse: CloseableHttpResponse = _
  var httpClientUserManager: HttpClientUserManager = _
  var httpClientServiceClient: HttpClientServiceClient = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    internalHttpClientService = mock[InternalHttpClientService]
    httpClient = mock[CloseableHttpClient]
    httpResponse = mock[CloseableHttpResponse]
    httpClientUserManager = mock[HttpClientUserManager]

    internalHttpClient = new InternalHttpClient(UUID.randomUUID.toString, httpClient)

    when(internalHttpClientService.getInternalClient(clientId)).thenReturn(internalHttpClient)

    httpClientServiceClient = new HttpClientServiceClient(internalHttpClientService, httpClientUserManager, clientId)
  }

  describe("execute") {
    it("should delegate request to the wrapped client") {
      val request = RequestBuilder.get("localhost").build()

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(request)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate request and context to the wrapped client") {
      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(request, context)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate target and request to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = RequestBuilder.get("localhost").build()

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(target, request)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate target, request, and context to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(target, request, context)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate request and response handler to the wrapped client") {
      val request = RequestBuilder.get("localhost").build()
      val responseHandler = new ResponseHandler[CloseableHttpResponse] {
        override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
      }

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(request, responseHandler)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate request, response handler, and context to the wrapped client") {
      val request = RequestBuilder.get("localhost").build()
      val context = CachingHttpClientContext.create()
      val responseHandler = new ResponseHandler[CloseableHttpResponse] {
        override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
      }

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(request, responseHandler, context)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate target, request, and response handler to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = RequestBuilder.get().build()
      val responseHandler = new ResponseHandler[CloseableHttpResponse] {
        override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
      }

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(target, request, responseHandler)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    it("should delegate target, request, response handler, and context to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = RequestBuilder.get().build()
      val context = CachingHttpClientContext.create()
      val responseHandler = new ResponseHandler[CloseableHttpResponse] {
        override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
      }

      when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenReturn(httpResponse)

      val response = httpClientServiceClient.execute(target, request, responseHandler, context)

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
      verify(httpClient).execute(
        any[HttpHost],
        isEq(request),
        any[HttpContext])
      verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      response shouldBe httpResponse
    }

    Seq(
      new IOException(),
      new ClientProtocolException(),
      new RuntimeException()
    ).foreach { exception =>
      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute request") {
        val request = RequestBuilder.get("localhost").build()

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(request)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute request and context") {
        val request = RequestBuilder.get("localhost").build()
        val context = CachingHttpClientContext.create()

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(request, context)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute target and request") {
        val target = HttpHost.create("localhost")
        val request = RequestBuilder.get().build()

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(target, request)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute target, request, and context") {
        val target = HttpHost.create("localhost")
        val request = RequestBuilder.get().build()
        val context = CachingHttpClientContext.create()

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(target, request, context)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute request and response handler") {
        val request = RequestBuilder.get("localhost").build()
        val responseHandler = new ResponseHandler[CloseableHttpResponse] {
          override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
        }

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(request, responseHandler)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute request, response handler, and context") {
        val request = RequestBuilder.get("localhost").build()
        val context = CachingHttpClientContext.create()
        val responseHandler = new ResponseHandler[CloseableHttpResponse] {
          override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
        }

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(request, responseHandler, context)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute target, request, and response handler") {
        val target = HttpHost.create("localhost")
        val request = RequestBuilder.get().build()
        val responseHandler = new ResponseHandler[CloseableHttpResponse] {
          override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
        }

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(target, request, responseHandler)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }

      it(s"should remove the user when an ${exception.getClass.getSimpleName} is thrown on execute target, request, response handler, and context") {
        val target = HttpHost.create("localhost")
        val request = RequestBuilder.get().build()
        val context = CachingHttpClientContext.create()
        val responseHandler = new ResponseHandler[CloseableHttpResponse] {
          override def handleResponse(response: HttpResponse): CloseableHttpResponse = response.asInstanceOf[CloseableHttpResponse]
        }

        when(httpClient.execute(any[HttpHost], any[HttpRequest], any[HttpContext])).thenThrow(exception)

        an[exception.type] should be thrownBy httpClientServiceClient.execute(target, request, responseHandler, context)

        verify(internalHttpClientService).getInternalClient(clientId)
        verify(httpClientUserManager).registerUser(isEq(internalHttpClient.getInstanceId), any[String])
        verify(httpClient).execute(
          any[HttpHost],
          isEq(request),
          any[HttpContext])
        verify(httpClientUserManager).deregisterUser(isEq(internalHttpClient.getInstanceId), any[String])
      }
    }
  }

  describe("getParams") {
    it("should delegate to the wrapped client") {
      httpClientServiceClient.getParams

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClient).getParams
    }
  }

  describe("getConnectionManager") {
    it("should delegate to the wrapped client") {
      httpClientServiceClient.getConnectionManager

      verify(internalHttpClientService).getInternalClient(clientId)
      verify(httpClient).getConnectionManager
    }
  }
}
