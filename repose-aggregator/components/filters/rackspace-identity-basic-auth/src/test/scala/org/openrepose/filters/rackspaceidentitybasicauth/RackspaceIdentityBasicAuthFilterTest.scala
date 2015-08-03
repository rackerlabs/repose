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
package org.openrepose.filters.rackspaceidentitybasicauth

import javax.servlet.http.HttpServletResponse

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.eclipse.jetty.server.Server
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.core.filter.logic.FilterAction
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.rackspaceidentitybasicauth.config.RackspaceIdentityBasicAuthConfig
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class RackspaceIdentityBasicAuthFilterTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {

  val identityServer = new Server(0)
  var listAppender: ListAppender = _
  var filterChain: MockFilterChain = _
  var mockDatastore: Datastore = _
  var mockDatastoreService: DatastoreService = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockConfigService: ConfigurationService = _
  var config: RackspaceIdentityBasicAuthConfig = _
  var filter: RackspaceIdentityBasicAuthFilter = _

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
    filterChain = new MockFilterChain
    mockDatastore = mock[Datastore]
    mockDatastoreService = mock[DatastoreService]
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockConfigService = mock[ConfigurationService]
    config = new RackspaceIdentityBasicAuthConfig
    filter = new RackspaceIdentityBasicAuthFilter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)
    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  }

  describe("the init method") {
    it("should be loud") {
      //val mockServletContext = new MockServletContext()
      val mockFilterConfig = new MockFilterConfig()

      // when:
      filter.init(mockFilterConfig)

      // then:
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("WARNING: This filter cannot be used alone, it requires an AuthFilter after it.")) shouldBe 1
    }
  }

  describe("the configurationUpdated method") {
    it("should be empty if field data is not present") {
      // when:
      filter.configurationUpdated(config)

      // then:
      assert(filter.isInitialized)
    }
  }

  // Due to a limitation of the current mock environment,
  // this test was moved to a Spock functional test.
  ignore("the doFilter method") {
    it("should be empty if field data is not present") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = new MockHttpServletRequest
      val mockServletResponse = new MockHttpServletResponse
      //when(mockServletResponse.getHeaderNames).thenReturn(new java.util.ArrayList[String])
      //when(mockServletResponse.extractHeaderValues).thenReturn(new util.ArrayList[String])//util.HashMap[HeaderName, List[HeaderValue]])

      // when:
      filter.configurationUpdated(config)
      filter.doFilter(mockServletRequest, mockServletResponse, filterChain)

      // then:
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("TEST")) shouldBe 1
    }
  }

  describe("handleRequest") {
    it("should simply pass if there is not an HTTP Basic authentication header") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = new MockHttpServletRequest
      val mockServletResponse = mock[ReadableHttpServletResponse]

      // when: "the filter's handleRequest() is called without an HTTP Basic authentication header"
      val filterDirector = filter.handleRequest(mockServletRequest, mockServletResponse)

      // then: "the filter's response status code would only be processed if it were set to UNAUTHORIZED (401) by another filter/service."
      filterDirector.getFilterAction equals FilterAction.PROCESS_RESPONSE
    }
  }

  // Due to a limitation of the current mock environment,
  // this test was moved to a Spock functional test.
  ignore("handleResponse") {
    it("should pass filter") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = new MockHttpServletRequest
      val mockServletResponse = mock[ReadableHttpServletResponse]
      //when(mockServletResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)

      // when: "the filter's/handler's handleResponse() is called"
      val filterDirector = filter.handleResponse(mockServletRequest, mockServletResponse)

      // then: "the filter's response status code should be No Content (204)"
      filterDirector.getFilterAction should not be (FilterAction.NOT_SET)
      filterDirector.getResponseStatusCode should be(HttpServletResponse.SC_NO_CONTENT)
    }
  }
}
