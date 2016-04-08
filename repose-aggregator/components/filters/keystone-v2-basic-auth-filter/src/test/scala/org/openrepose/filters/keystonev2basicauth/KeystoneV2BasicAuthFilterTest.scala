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
package org.openrepose.filters.keystonev2basicauth

import javax.servlet.http.HttpServletResponse

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.filters.keystonev2basicauth.config.KeystoneV2BasicAuthConfig
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class KeystoneV2BasicAuthFilterTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {

  var listAppender: ListAppender = _
  var filterChain: MockFilterChain = _
  var mockDatastore: Datastore = _
  var mockDatastoreService: DatastoreService = _
  var mockAkkaServiceClient: AkkaServiceClient = _
  var mockAkkaServiceClientFactory: AkkaServiceClientFactory = _
  var mockConfigService: ConfigurationService = _
  var config: KeystoneV2BasicAuthConfig = _
  var filter: KeystoneV2BasicAuthFilter = _

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
    filterChain = new MockFilterChain
    mockDatastore = mock[Datastore]
    mockDatastoreService = mock[DatastoreService]
    mockAkkaServiceClient = mock[AkkaServiceClient]
    mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
    mockConfigService = mock[ConfigurationService]
    config = new KeystoneV2BasicAuthConfig
    filter = new KeystoneV2BasicAuthFilter(mockConfigService, mockAkkaServiceClientFactory, mockDatastoreService)

    when(mockDatastore.get(anyString)).thenReturn(null, Nil: _*)
    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
    when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)
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

    it("should destroy the previous akka service client") {
      // given: two different clients will be returned on subsequent calls to the factory to create new instances
      val firstAkkaServiceClient = mock[AkkaServiceClient]
      val secondAkkaServiceClient = mock[AkkaServiceClient]
      when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String])))
        .thenReturn(firstAkkaServiceClient)
        .thenReturn(secondAkkaServiceClient)

      // when: configuration is updated twice
      filter.configurationUpdated(config)
      filter.configurationUpdated(config)

      // then: the factory is used twice, and the previous client is destroyed
      verify(mockAkkaServiceClientFactory, times(2)).newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))
      verify(firstAkkaServiceClient, times(1)).destroy()
      verify(secondAkkaServiceClient, never()).destroy()
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
      val mockServletResponse = new MockHttpServletResponse

      // when: "the filter's handleRequest() is called without an HTTP Basic authentication header"
      val filterAction = filter.handleRequest(new HttpServletRequestWrapper(mockServletRequest), mockServletResponse)

      // then: "the filter's response status code would only be processed if it were set to UNAUTHORIZED (401) by another filter/service."
      filterAction shouldBe FilterAction.PROCESS_RESPONSE
    }
  }

  // Due to a limitation of the current mock environment,
  // this test was moved to a Spock functional test.
  ignore("handleResponse") {
    it("should pass filter") {
      // given: "a mock'd ServletRequest and ServletResponse"
      val mockServletRequest = new MockHttpServletRequest
      val mockServletResponse = new MockHttpServletResponse
      //when(mockServletResponse.getStatus).thenReturn(HttpServletResponse.SC_OK)

      // when: "the filter's/handler's handleResponse() is called"
      val filterAction = filter.handleResponse(mockServletRequest, mockServletResponse)

      // then: "the filter's response status code should be No Content (204)"
      filterAction should not be FilterAction.NOT_SET
      mockServletResponse.getStatus should be(HttpServletResponse.SC_NO_CONTENT)
    }
  }

  describe("when destroying the filter") {
    it("should destroy the akka service client") {
      // given: the akka service client exists
      filter.configurationUpdated(config)

      // when: the filter is destroyed
      filter.destroy()

      // then: the akka service client is destroyed, too
      verify(mockAkkaServiceClient).destroy()
    }
  }
}
