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

package org.openrepose.filters.uristripper

import javax.servlet.FilterChain

import org.apache.http.HttpHeaders
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.openrepose.filters.uristripper.config.UriStripperConfig
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class UriStripperFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import UriStripperFilterTest._

  var filter: UriStripperFilter = _
  var request: MockHttpServletRequest = _
  var response: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    request = new MockHttpServletRequest
    response = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    filter = new UriStripperFilter(null)
  }

  describe("URI stripping") {
    List(
      // @formatter:off
      ("/v1/12345/woot/butts" , 1 , "/v1/woot/butts"),
      ("/path/to/your/mom"    , 3 , "/path/to/your" ),
      ("/lol/butts"           , 0 , "/butts"        ),
      ("/lol/butts"           , 1 , "/lol"          ),
      ("/lol/butts"           , 2 , "/lol/butts"    ),
      ("/"                    , 0 , "/"             ),
      ("/"                    , 1 , "/"             ),
      ("/v2"                  , 0 , "/"             ),
      ("/v2"                  , 1 , "/v2"           )
      // @formatter:on
    ) foreach { case (resourcePath, index, strippedPath) =>
      it(s"properly strips the URI $resourcePath with configured index $index to $strippedPath") {
        filter.configurationUpdated(createConfig(index = index))
        request.setRequestURI(resourcePath)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequest.getRequestURI shouldBe strippedPath
      }
    }
  }

  describe("location header") {
    List(
      // @formatter:off
      ("/v1/12345/some/resource"    , 1 , "http://example.com/v1/some/resource"            , "http://example.com/v1/12345/some/resource"           ), // before token
      ("/your/mom/is/a/classy/lady" , 3 , "http://example.com/your/mom/is/classy/lady"     , "http://example.com/your/mom/is/a/classy/lady"        ), // before token
      ("/product/123/item/123"      , 1 , "http://example.com/product/item/123"            , "http://example.com/product/123/item/123"             ), // before token
      ("/product/123/item/456"      , 3 , "http://example.com/product/123/item"            , "http://example.com/product/123/item/456"             ), // before token, last index
      ("/v1/12345/path/to/resource" , 1 , "http://service.com/v1/path/to/resource?a=b&c=d" , "http://service.com/v1/12345/path/to/resource?a=b&c=d"), // before token, query params
      ("/v1/12345/path/to/resource" , 1 , "/v1/path/to/resource?a=b&c=d"                   , "/v1/12345/path/to/resource?a=b&c=d"                  ), // before token, URI-only Location header
      ("/v1/servers/r789/status"    , 2 , "http://service.com/status"                      , "http://service.com/r789/status"                      ), // after token
      ("/v1/servers/r789/status"    , 2 , "http://service.com/status?server=r789"          , "http://service.com/r789/status?server=r789"          ), // after token, query params
      ("/r789/status"               , 0 , "http://service.com/server/status"               , "http://service.com/server/r789/status"               ), // after token, first index
      ("/v1/datastores/d913/delete" , 2 , "http://service.com/unrelated/url/lol"           , "http://service.com/unrelated/url/lol"                ), // no token in Location header
      ("/v1/servers"                , 2 , "http://service.com/server/all"                  , "http://service.com/server/all"                       )  // no token at all
      // @formatter:on
    ) foreach { case (resourcePath, index, originalLocation, newLocation) =>
      it(s"properly updates Location header $originalLocation to $newLocation for URI $resourcePath with index $index") {
        filter.configurationUpdated(createConfig(index = index, rewriteLocationHeader = true))
        request.setRequestURI(resourcePath)
        addResponseHeaders(List(SimpleHeader(HttpHeaders.LOCATION, originalLocation)))

        filter.doFilter(request, response, filterChain)

        response.getHeader(HttpHeaders.LOCATION) shouldBe newLocation
      }
    }

    it("does not update the Location header when it has an invalid URL") {
      val invalidLocation = "http://example.com/v1/some(\\/resource"
      filter.configurationUpdated(createConfig(index = 1, rewriteLocationHeader = true))
      request.setRequestURI("/v1/12345/some/resource")
      addResponseHeaders(List(SimpleHeader(HttpHeaders.LOCATION, invalidLocation)))

      filter.doFilter(request, response, filterChain)

      response.getHeader(HttpHeaders.LOCATION) shouldBe invalidLocation
    }
  }

  def createConfig(index: Int = 0, rewriteLocationHeader: Boolean = false): UriStripperConfig = {
    val config = new UriStripperConfig
    config.setRewriteLocation(rewriteLocationHeader)
    config.setTokenIndex(index)
    config
  }

  /**
    * Returns the wrapped request that was passed in to the filterChain
    */
  def getPostFilterRequest: HttpServletRequestWrapper = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[MockHttpServletResponse]))
    requestCaptor.getValue
  }

  /**
    * Grabs the wrapped response that was passed in to the filterChain and adds headers to it as if we were the next filter
    */
  def addResponseHeaders(headers: Iterable[SimpleHeader]): Unit = {
    val responseCaptor = ArgumentCaptor.forClass(classOf[HttpServletResponseWrapper])
    doAnswer(new Answer[Void]() {
      def answer(invocation: InvocationOnMock): Void = {
        headers.foreach(header => responseCaptor.getValue.addHeader(header.name, header.value))
        null
      }
    }).when(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), responseCaptor.capture())
  }
}

object UriStripperFilterTest {

  case class SimpleHeader(name: String, value: String)

}
