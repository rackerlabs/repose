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

import java.net.URI

import akka.http.scaladsl.model._
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.docs.repose.atom_feed_service.v1.OpenStackIdentityV2AuthenticationType
import org.openrepose.nodeservice.atomfeed.impl.MockService
import org.openrepose.nodeservice.atomfeed.{AuthenticationRequestException, FeedReadRequest}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV2AuthenticatedRequestFactoryTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var mockIdentityService: MockService = _
  var osiarf: OpenStackIdentityV2AuthenticatedRequestFactory = _

  override def beforeEach() = {
    mockIdentityService = new MockService()
  }

  def finishSetup(): Unit = {
    mockIdentityService.start()

    val osiat = new OpenStackIdentityV2AuthenticationType()
    osiat.setUsername("usr")
    osiat.setPassword("pwd")
    osiat.setUri(mockIdentityService.getUrl)

    osiarf = new OpenStackIdentityV2AuthenticatedRequestFactory(osiat)
  }

  describe("authenticateRequest") {
    it("should add a tracing header to the request to Identity") {
      var requestHeaders: Seq[HttpHeader] = Seq.empty

      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), headers, _, _) =>
          requestHeaders = headers
          HttpResponse(StatusCodes.BadRequest)
      }

      finishSetup()

      val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }

      requestHeaders.exists(_.is(CommonHttpHeader.TRACE_GUID.toString)) shouldBe true
    }

    it("should handle a non-JSON response") {
      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          HttpResponse(entity = HttpEntity(ContentTypes.`text/plain`, """access.token.id=test-token"""))
      }

      finishSetup()

      val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
    }

    it("should handle a 4xx response") {
      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          HttpResponse(StatusCodes.BadRequest)
      }

      finishSetup()

      val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      intercept[AuthenticationRequestException] {
        osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      }
    }

    it("should send a valid payload and receive a valid token for the user provided") {
      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"access":{"token":{"id":"test-token"}}}"""))
      }

      finishSetup()

      val feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))

      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
    }

    it("should cache a token until invalidated") {
      var numberOfInterations = 0

      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          numberOfInterations += 1
          HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"access":{"token":{"id":"test-token"}}}"""))
      }

      finishSetup()

      var feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      numberOfInterations shouldEqual 1

      feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      numberOfInterations shouldEqual 1

      osiarf.onInvalidCredentials()

      feedReadRequest = new FeedReadRequest(new URI("http://example.com"))

      osiarf.authenticateRequest(feedReadRequest, AuthenticationRequestContextImpl("", ""))
      feedReadRequest.getHeaders.get(CommonHttpHeader.AUTH_TOKEN.toString) should contain only "test-token"
      numberOfInterations shouldEqual 2
    }
  }
}
