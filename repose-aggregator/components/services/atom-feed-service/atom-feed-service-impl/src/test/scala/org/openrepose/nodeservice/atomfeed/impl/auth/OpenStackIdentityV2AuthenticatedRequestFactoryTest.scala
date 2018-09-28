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
package org.openrepose.nodeservice.atomfeed.impl.auth

import java.io.ByteArrayInputStream
import java.net.URI

import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.MediaType
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.{Header, HttpVersion}
import org.apache.http.client.methods.{HttpPost, HttpUriRequest}
import org.apache.http.message.{BasicHeader, BasicHttpResponse}
import org.apache.http.protocol.HttpContext
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.{AuthenticationRequestException, FeedReadRequest}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV2AuthenticatedRequestFactoryTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

  var mockHttpClientService: HttpClientService = _
  var mockHttpClient: HttpClientServiceClient = _
  var alsoMockHttpClient: HttpClientServiceClient = _
  var osiarf: OpenStackIdentityV2AuthenticatedRequestFactory = _

  override def beforeEach() = {
    feedReadRequest.setURI(new URI("http://example.com"))
    feedReadRequest.getHeaders.clear()

    mockHttpClientService = mock[HttpClientService]
    mockHttpClient = mock[HttpClientServiceClient]
  }

  describe("authenticateRequest") {
    def finishSetup(): Unit = {
      when(mockHttpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)

      val afct = new AtomFeedConfigType
      val osiat = new OpenStackIdentityV2AuthenticationType
      osiat.setUsername("usr")
      osiat.setPassword("pwd")

      osiarf = new OpenStackIdentityV2AuthenticatedRequestFactory(afct, osiat, mockHttpClientService)
    }

    it("should add a tracing header to the request to Identity") {
      finishSetup()

      intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(mockHttpClient).execute(requestCaptor.capture(), any[HttpContext])
      requestCaptor.getValue.getMethod shouldEqual HttpPost.METHOD_NAME
      requestCaptor.getValue.containsHeader(CommonHttpHeader.TRACE_GUID) shouldBe true
    }

    it("should handle a non-JSON response") {
      val responseEntity = EntityBuilder.create()
        .setBinary("access.token.id=test-token".getBytes)
        .build()
      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_OK, null)
      response.addHeader("ContentTypes", "text/plain")
      response.setEntity(responseEntity)

      when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext]))
        .thenReturn(response)
      finishSetup()

      val thrown = intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
      Option(thrown).isDefined shouldBe true
    }

    it("should handle a 4xx response") {
      val responseEntity = EntityBuilder.create()
        .setBinary("BODY".getBytes)
        .build()
      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_FORBIDDEN, null)
      response.addHeader("ContentTypes", "text/plain")
      response.setEntity(responseEntity)

      when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext]))
        .thenReturn(response)
      finishSetup()

      val thrown = intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
      thrown.getCause.getMessage.contains(Integer.toString(SC_FORBIDDEN)) shouldBe true
    }

    it("should send a valid payload and receive a valid token for the user provided") {
      val responseEntity = EntityBuilder.create()
        .setBinary("""{"access":{"token":{"id":"test-token"}}}""".getBytes)
        .build()
      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_OK, null)
      response.addHeader("ContentTypes", "application/json")
      response.setEntity(responseEntity)

      when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext]))
        .thenReturn(response)
      finishSetup()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))

      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN) should contain only "test-token"
    }

    it("should cache a token until invalidated") {
      def resetAkkaServiceClient = {
        val responseEntity = EntityBuilder.create()
          .setBinary("""{"access":{"token":{"id":"test-token"}}}""".getBytes)
          .build()
        val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, SC_OK, null)
        response.addHeader("ContentTypes", "application/json")
        response.setEntity(responseEntity)

        when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext]))
          .thenReturn(response)
        when(mockHttpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)
      }
      finishSetup()
      resetAkkaServiceClient

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN) should contain only "test-token"
      verify(mockHttpClient, times(1)).execute(any[HttpUriRequest], any[HttpContext])
      resetAkkaServiceClient

      feedReadRequest.setURI(new URI("http://example.com"))
      feedReadRequest.getHeaders.clear()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN) should contain only "test-token"
      verify(mockHttpClient, times(1)).execute(any[HttpUriRequest], any[HttpContext])
      resetAkkaServiceClient

      osiarf.onInvalidCredentials()

      feedReadRequest.setURI(new URI("http://example.com"))
      feedReadRequest.getHeaders.clear()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN) should contain only "test-token"
      verify(mockHttpClient, times(2)).execute(any[HttpUriRequest], any[HttpContext])
    }
  }
}
