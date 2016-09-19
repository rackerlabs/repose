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

import org.apache.http.Header
import org.apache.http.message.BasicHeader
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.{AuthenticationRequestException, FeedReadRequest}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV2AuthenticatedRequestFactoryTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

  var mockAkkaServiceClient: AkkaServiceClient = _
  var alsoAkkaServiceClient: AkkaServiceClient = _
  var mockAkkaServiceClientFactory: AkkaServiceClientFactory = _
  var osiarf: OpenStackIdentityV2AuthenticatedRequestFactory = _

  override def beforeEach() = {
    feedReadRequest.setURI(new URI("http://example.com"))
    feedReadRequest.getHeaders.clear()

    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
  }

  describe("authenticateRequest") {
    def finishSetup(): Unit = {
      when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)

      val afct = new AtomFeedConfigType
      val osiat = new OpenStackIdentityV2AuthenticationType
      osiat.setUsername("usr")
      osiat.setPassword("pwd")

      osiarf = new OpenStackIdentityV2AuthenticatedRequestFactory(afct, osiat, mockAkkaServiceClientFactory)
    }

    it("should add a tracing header to the request to Identity") {
      finishSetup()

      intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }

      val headersCaptor = ArgumentCaptor.forClass(classOf[java.util.Map[String, String]])
      verify(mockAkkaServiceClient).post(anyString(), anyString(), headersCaptor.capture(), anyString(), any[MediaType](), anyBoolean())
      headersCaptor.getValue.containsKey(CommonHttpHeader.TRACE_GUID.toString) shouldBe true
    }

    it("should handle a non-JSON response") {
      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean()))
        .thenReturn(new ServiceClientResponse(
          SC_OK,
          Array[Header](new BasicHeader("ContentTypes", "text/plain")),
          new ByteArrayInputStream("access.token.id=test-token".getBytes)))
      finishSetup()

      val thrown = intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
      Option(thrown).isDefined shouldBe true
    }

    it("should handle a 4xx response") {
      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean()))
        .thenReturn(new ServiceClientResponse(
          SC_FORBIDDEN,
          Array[Header](new BasicHeader("ContentTypes", "text/plain")),
          new ByteArrayInputStream("BODY".getBytes)))
      finishSetup()

      val thrown = intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
      thrown.getCause.getMessage.contains(Integer.toString(SC_FORBIDDEN)) shouldBe true
    }

    it("should send a valid payload and receive a valid token for the user provided") {
      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean()))
        .thenReturn(new ServiceClientResponse(
          SC_OK,
          Array[Header](new BasicHeader("ContentTypes", "application/json")),
          new ByteArrayInputStream("""{"access":{"token":{"id":"test-token"}}}""".getBytes)))
      finishSetup()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))

      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
    }

    it("should cache a token until invalidated") {
      def resetAkkaServiceClient = {
        when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean()))
          .thenReturn(new ServiceClientResponse(
            SC_OK,
            Array[Header](new BasicHeader("ContentTypes", "application/json")),
            new ByteArrayInputStream("""{"access":{"token":{"id":"test-token"}}}""".getBytes)))
        when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)
      }
      finishSetup()
      resetAkkaServiceClient

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      verify(mockAkkaServiceClient, times(1)).post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean())
      resetAkkaServiceClient

      feedReadRequest.setURI(new URI("http://example.com"))
      feedReadRequest.getHeaders.clear()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      verify(mockAkkaServiceClient, times(1)).post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean())
      resetAkkaServiceClient

      osiarf.onInvalidCredentials()

      feedReadRequest.setURI(new URI("http://example.com"))
      feedReadRequest.getHeaders.clear()

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      verify(mockAkkaServiceClient, times(2)).post(anyString(), anyString(), anyMapOf[String, String](classOf[String], classOf[String]), anyString(), any[MediaType](), anyBoolean())
    }

    it("should destroy the akka service client when destroying the AuthenticatedRequestFactory") {
      finishSetup()

      // when: the filter is destroyed
      osiarf.destroy()

      // then: the akka service client is destroyed, too
      verify(mockAkkaServiceClient).destroy()
    }
  }
}
