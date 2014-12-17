package org.openrepose.filters.herp

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.openrepose.filters.herp.config.HerpConfig
import org.scalatest._
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

class HerpFilterTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfter with Matchers {

  var herpFilter: HerpFilter = _
  var herpConfig: HerpConfig = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var listAppender: ListAppender = _

  override def beforeAll() {
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
      "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
  }

  //todo: replace this and the log4j2 config with programmatic modification to the root logger with a mock appender?
  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("highly-efficient-record-processor-ListAppender").asInstanceOf[ListAppender].clear
  }

  describe("the doFilter method") {
    it("should log null if field data is not present") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex "\"ServiceCode\":null.*\"URL\":null"
    }
    it("should log a guid") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex "\"GUID\":\".+\""
    }
    it("should log the configured service code") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      herpConfig.setServiceCode("some-service")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ServiceCode\":\"some-service\"")
    }
    it("should log the configured region") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      herpConfig.setRegion("some-region")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Region\":\"some-region\"")
    }
    it("should log the configured data center") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      herpConfig.setDataCenter("some-data-center")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"DataCenter\":\"some-data-center\"")
    }
    it("should extract and log the request method") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Method\":\"POST\"")
    }
    it("should extract and log the request url") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.setAttribute("http://openrepose.org/requestUrl", "http://foo.com")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"URL\":\"http://foo.com\"")
    }
    it("should extract and log the request parameters") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.setAttribute("http://openrepose.org/queryParams", Map("foo" -> Array("bar", "baz")).asJava)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Parameters\":{\"foo\":[\"bar\",\"baz\"]}")
    }
    it("should extract and log the request user name header") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.addHeader("X-User-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"UserName\":\"foo\"")
    }
    it("should extract and log the request impersonator name header") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.addHeader("X-Impersonator-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ImpersonatorName\":\"foo\"")
    }
    it("should extract and log the request tenant id header") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.addHeader("X-Tenant-Id", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"TenantID\":\"foo\"")
    }
    it("should extract and log the request roles header") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.addHeader("X-Roles", "foo")
      servletRequest.addHeader("X-Roles", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Roles\":[\"foo\",\"bar\"]")
    }
    it("should extract and log the request user agent") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.addHeader("User-Agent", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"UserAgent\":\"foo\"")
    }
    it("should extract and log the response code") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletResponse.setStatus(418)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Code\":418")
    }
    it("should extract and log the response message") {
      // given:
      val herpFilter = new HerpFilter
      val herpConfig = new HerpConfig
      val servletRequest = new MockHttpServletRequest
      val servletResponse = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletResponse.setStatus(418, "I'm a teapot")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Message\":\"IM_A_TEAPOT\"")
    }
    ignore("should extract and log the response body") {
      //given:
      val herpFilter = new HerpFilter()
      val herpConfig = new HerpConfig()
      val servletRequest = new MockHttpServletRequest()
      val servletResponse = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      herpConfig.setId("highly-efficient-record-processor-Logger")

      val responseBody = "HEY A BODY"
      servletResponse.setContentLength(responseBody.length)
      servletResponse.getWriter.write(responseBody)
      servletResponse.getWriter.flush()
      servletResponse.getWriter.close()

      //when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      //then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("HEY A BODY")
    }
  }
}
