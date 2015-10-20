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

import java.net.URLConnection

import akka.http.scaladsl.model._
import org.junit.runner.RunWith
import org.mockito.Mockito.{times, verify}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.docs.repose.atom_feed_service.v1.OpenStackIdentityAuthenticationType
import org.openrepose.nodeservice.atomfeed.impl.actors.MockService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityAuthenticatedRequestFactoryTest
  extends FunSpec with BeforeAndAfter with MockitoSugar with Matchers {

  var mockIdentityService: MockService = _
  var osiarf: OpenStackIdentityAuthenticatedRequestFactory = _

  before {
    mockIdentityService = new MockService()
  }

  def finishSetup(): Unit = {
    mockIdentityService.start()

    val osiat = new OpenStackIdentityAuthenticationType()
    osiat.setUsername("usr")
    osiat.setPassword("pwd")
    osiat.setUri(mockIdentityService.getUrl)

    osiarf = new OpenStackIdentityAuthenticatedRequestFactory(osiat)
  }

  describe("authenticateRequest") {
    it("should handle a 4xx response") {
      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          HttpResponse(StatusCodes.BadRequest)
      }

      finishSetup()

      val mockConnection = mock[URLConnection]
      osiarf.authenticateRequest(mockConnection) shouldBe null
    }

    it("should send a valid payload and receive a valid token for the user provided") {
      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"access":{"token":{"id":"test-token"}}}"""))
      }

      finishSetup()

      val mockConnection = mock[URLConnection]
      osiarf.authenticateRequest(mockConnection)

      verify(mockConnection).setRequestProperty(CommonHttpHeader.AUTH_TOKEN.toString, "test-token")
    }

    it("should cache a token until invalidated") {
      var numberOfInterations = 0

      mockIdentityService.requestHandler = {
        case HttpRequest(_, Uri.Path("/v2.0/tokens"), _, _, _) =>
          numberOfInterations += 1
          HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, """{"access":{"token":{"id":"test-token"}}}"""))
      }

      finishSetup()

      val mockConnection = mock[URLConnection]

      osiarf.authenticateRequest(mockConnection)
      verify(mockConnection).setRequestProperty(CommonHttpHeader.AUTH_TOKEN.toString, "test-token")
      numberOfInterations shouldEqual 1

      osiarf.authenticateRequest(mockConnection)
      verify(mockConnection, times(2)).setRequestProperty(CommonHttpHeader.AUTH_TOKEN.toString, "test-token")
      numberOfInterations shouldEqual 1

      osiarf.invalidateCache()

      osiarf.authenticateRequest(mockConnection)
      verify(mockConnection, times(3)).setRequestProperty(CommonHttpHeader.AUTH_TOKEN.toString, "test-token")
      numberOfInterations shouldEqual 2
    }
  }
}
