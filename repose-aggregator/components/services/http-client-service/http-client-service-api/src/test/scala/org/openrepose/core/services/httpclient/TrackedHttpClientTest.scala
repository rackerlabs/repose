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

import java.util.UUID

import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.{HttpClient, ResponseHandler}
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHost, HttpRequest}
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TrackedHttpClientTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var httpClient: HttpClient = _
  var trackedHttpClientAdapter: TrackedHttpClient = _

  override def beforeEach(): Unit = {
    super.beforeEach()

    httpClient = mock[HttpClient]
    trackedHttpClientAdapter = new TrackedHttpClient(httpClient, "", "")
  }

  describe("testGetClientInstanceId") {
    it("should return the set client instance ID") {
      val clientInstanceId = UUID.randomUUID().toString
      val trackedHttpClient = new TrackedHttpClient(httpClient, clientInstanceId, "")

      trackedHttpClient.getClientInstanceId shouldEqual clientInstanceId
    }
  }

  describe("testGetUserId") {
    it("should return the set user ID") {
      val userId = UUID.randomUUID().toString
      val trackedHttpClient = new TrackedHttpClient(httpClient, "", userId)

      trackedHttpClient.getUserId shouldEqual userId
    }
  }

  describe("execute") {
    it("should delegate request to the wrapped client") {
      val request = mock[HttpUriRequest]
      trackedHttpClientAdapter.execute(request)

      verify(httpClient).execute(request)
    }

    it("should delegate request and context to the wrapped client") {
      val request = mock[HttpUriRequest]
      val context = mock[HttpContext]
      trackedHttpClientAdapter.execute(request, context)

      verify(httpClient).execute(request, context)
    }

    it("should delegate target and request to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = mock[HttpRequest]
      trackedHttpClientAdapter.execute(target, request)

      verify(httpClient).execute(target, request)
    }

    it("should delegate target, request, and context to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = mock[HttpRequest]
      val context = mock[HttpContext]
      trackedHttpClientAdapter.execute(target, request, context)

      verify(httpClient).execute(target, request, context)
    }

    it("should delegate request and response handler to the wrapped client") {
      val request = mock[HttpUriRequest]
      val responseHandler = mock[ResponseHandler[_]]
      trackedHttpClientAdapter.execute(request, responseHandler)

      verify(httpClient).execute(request, responseHandler)
    }

    it("should delegate request, response handler, and context to the wrapped client") {
      val request = mock[HttpUriRequest]
      val responseHandler = mock[ResponseHandler[_]]
      val context = mock[HttpContext]
      trackedHttpClientAdapter.execute(request, responseHandler, context)

      verify(httpClient).execute(request, responseHandler, context)
    }

    it("should delegate target, request, and response handler to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = mock[HttpRequest]
      val responseHandler = mock[ResponseHandler[_]]
      trackedHttpClientAdapter.execute(target, request, responseHandler)

      verify(httpClient).execute(target, request, responseHandler)
    }

    it("should delegate target, request, response handler, and context to the wrapped client") {
      val target = HttpHost.create("localhost")
      val request = mock[HttpRequest]
      val responseHandler = mock[ResponseHandler[_]]
      val context = mock[HttpContext]
      trackedHttpClientAdapter.execute(target, request, responseHandler, context)

      verify(httpClient).execute(target, request, responseHandler, context)
    }
  }

  describe("getParams") {
    it("should delegate to the wrapped client") {
      trackedHttpClientAdapter.getParams

      verify(httpClient).getParams
    }
  }

  describe("getConnectionManager") {
    it("should delegate to the wrapped client") {
      trackedHttpClientAdapter.getConnectionManager

      verify(httpClient).getConnectionManager
    }
  }
}
