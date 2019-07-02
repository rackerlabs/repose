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

package org.openrepose.filters.uriuser

import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.PowerApiHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.uriuser.config.{IdentificationMapping, IdentificationMappingList, UriUserConfig}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class UriUserFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import UriUserFilterTest._

  var filter: UriUserFilter = _
  var request: MockHttpServletRequest = _
  var response: ServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    request = new MockHttpServletRequest
    response = mock[ServletResponse]
    filterChain = mock[FilterChain]

    filter = new UriUserFilter(null)
  }

  describe("the user header") {
    it("should NOT be added if there are no configured URIs at all") {
      filter.configurationUpdated(createConfig())
      request.setRequestURI("/v1/servers")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(User) shouldBe null
    }

    it("should NOT be added if there are no configured URIs that match") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/datastores/([^/]+)", "/v2/(.*)")))
      request.setRequestURI("/v1/servers")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(User) shouldBe null
    }

    it("should NOT be added if the matching URI regex did not include a capture group") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/[^/]+")))
      request.setRequestURI("/v1/servers/r3829982")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(User) shouldBe null
    }

    it("should be added if there is a matching configured URI") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/([^/]+)")))
      request.setRequestURI("/v1/servers/r3829982")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(User) shouldBe "r3829982" + DefaultQuality
    }

    it("should be added when the second configured URI matches") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v2/(.*)", "/v1/servers/([^/]+)")))
      request.setRequestURI("/v1/servers/r0981254")

      filter.doFilter(request, response, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(User) shouldBe "r0981254" + DefaultQuality
      postFilterRequest.getHeadersScala(User).size shouldBe 1
    }
  }

  describe("the group header") {
    it("should NOT be added if there are no configured URIs at all") {
      filter.configurationUpdated(createConfig(group = Some("party-animals")))
      request.setRequestURI("/v1/servers")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(Groups) shouldBe null
    }

    it("should NOT be added if there are no configured URIs that match") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/datastores/([^/]+)", "/v2/(.*)"), group = Some("vegetables")))
      request.setRequestURI("/v1/servers")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(Groups) shouldBe null
    }

    it("should NOT be added if the matching URI regex did not include a capture group") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/[^/]+"), group = Some("plants")))
      request.setRequestURI("/v1/servers/r3829982")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(Groups) shouldBe null
    }

    it("should be added if there is a matching configured URI") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/([^/]+)"), group = Some("sailors")))
      request.setRequestURI("/v1/servers/r3829982")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(Groups) shouldBe "sailors" + DefaultQuality
    }

    it("should default to 'User_Standard' if there is no configured group") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/([^/]+)")))
      request.setRequestURI("/v1/servers/r0981254")

      filter.doFilter(request, response, filterChain)

      getPostFilterRequest.getHeader(Groups) shouldBe "User_Standard" + DefaultQuality
    }
  }

  describe("the header quality") {
    it("should be added to the user and group headers using the configured value") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/([^/]+)"), quality = Some(0.2d)))
      request.setRequestURI("/v1/servers/r3829982")

      filter.doFilter(request, response, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(User) should endWith (";q=0.2")
      postFilterRequest.getHeader(Groups) should endWith (";q=0.2")
    }

    it("should default to 0.5 when not configured") {
      filter.configurationUpdated(createConfig(uriPatterns = List("/v1/servers/([^/]+)")))
      request.setRequestURI("/v1/servers/r0981254")

      filter.doFilter(request, response, filterChain)

      val postFilterRequest = getPostFilterRequest
      postFilterRequest.getHeader(User) should endWith (";q=0.5")
      postFilterRequest.getHeader(Groups) should endWith (";q=0.5")
    }
  }

  def createConfig(uriPatterns: Seq[String] = Seq.empty, group: Option[String] = None, quality: Option[Double] = None): UriUserConfig = {
    def createIdMapping(pattern: String): IdentificationMapping = {
      val identificationMapping = new IdentificationMapping
      identificationMapping.setIdentificationRegex(pattern)
      identificationMapping
    }

    val uriUserConfig = new UriUserConfig
    val identificationMappingList = new IdentificationMappingList
    uriUserConfig.setIdentificationMappings(identificationMappingList)

    uriPatterns.map(createIdMapping).foreach(identificationMappingList.getMapping.add)
    group.foreach(uriUserConfig.setGroup)
    quality.foreach(uriUserConfig.setQuality(_))

    uriUserConfig
  }

  /**
    * Get the wrapped HTTP servlet request passed to the filter chain
    */
  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    requestCaptor.getValue
  }
}

object UriUserFilterTest {
  private final val User = PowerApiHeader.USER
  private final val Groups = PowerApiHeader.GROUPS
  private final val DefaultQuality = ";q=0.5"
}
