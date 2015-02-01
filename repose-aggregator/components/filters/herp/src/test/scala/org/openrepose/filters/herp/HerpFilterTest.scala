package org.openrepose.filters.herp

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.filters.herp.config.{HerpConfig, Template}
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
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

    herpFilter = new HerpFilter
    herpConfig = new HerpConfig
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain
    herpConfig.setPreFilterLoggerName("highly-efficient-record-processor-Logger")
    val templateText =
      """
         {
          "GUID" : "{{guid}}",
          "ServiceCode" : "{{serviceCode}}",
          "Region" : "{{region}}",
          "DataCenter" : "{{dataCenter}}",
          "Timestamp" : "{{timestamp}}",
          "Request" : {
            "Method" : "{{requestMethod}}",
            "URL" : "{{requestURL}}",
            "QueryString" : "{{requestQueryString}}",
            "Parameters" : { {{#each parameters}}{{#if @index}},{{/if}}"{{key}}" : [{{#each value}}{{#if @index}},{{/if}}"{{.}}"{{/each}}]{{/each}}
                           },
            "UserName" : "{{userName}}",
            "ImpersonatorName" : "{{impersonatorName}}",
            "ProjectID" : [
                            {{#each projectID}}
                            {{#if @index}},{{/if}}"{{.}}"
                            {{/each}}
                          ],
            "Roles" : [
                        {{#each roles}}
                        {{#if @index}},{{/if}}"{{.}}"
                        {{/each}}
                      ],
            "UserAgent" : "{{userAgent}}"
          },
          "Response" : {
            "Code" : {{responseCode}},
            "Message" : "{{responseMessage}}"
          }
         }
      """.stripMargin
    val template = new Template
    template.setValue(templateText)
    template.setCrush(true)
    herpConfig.setTemplate(template)
  }

  describe("the doFilter method") {
    it("should be empty if field data is not present") {
      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex "\"ServiceCode\" : \"\".*\"URL\" : \"\""
    }
    it("should log a guid") {
      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex "\"GUID\" : \".+\""
    }
    it("should log the configured service code") {
      // given:
      herpConfig.setServiceCode("some-service")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ServiceCode\" : \"some-service\"")
    }
    it("should log the configured region") {
      // given:
      herpConfig.setRegion("some-region")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Region\" : \"some-region\"")
    }
    it("should log the configured data center") {
      // given:
      herpConfig.setDataCenter("some-data-center")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"DataCenter\" : \"some-data-center\"")
    }
    it("should extract and log the request method") {
      // given:
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Method\" : \"POST\"")
    }
    it("should extract and log the request url") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/requestUrl", "http://foo.com")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"URL\" : \"http://foo.com\"")
    }
    it("should extract and log the unaltered request query string") {
      // given:
      servletRequest.setQueryString("a=b&c=d%20e")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"QueryString\" : \"a=b&amp;c=d%20e\"")
    }
    it("should extract and log the request parameters") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/queryParams", Map("foo" -> Array("bar", "baz")).asJava)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Parameters\" : { \"foo\" : [\"bar\",\"baz\"] }")
    }
    it("should extract and log the decoded request parameters") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/queryParams", Map("foo%20bar" -> Array("baz%20test")).asJava)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Parameters\" : { \"foo bar\" : [\"baz test\"] }")
    }
    it("should extract and log the request user name header") {
      // given:
      servletRequest.addHeader("X-User-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"UserName\" : \"foo\"")
    }
    it("should extract and log the request impersonator name header") {
      // given:
      servletRequest.addHeader("X-Impersonator-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ImpersonatorName\" : \"foo\"")
    }
    it("should extract and log the request tenant id header") {
      // given:
      servletRequest.addHeader("X-Tenant-Id", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ProjectID\" : [  \"foo\"  ]")
    }
    it("should extract and log multiple tenant id header values") {
      // given:
      servletRequest.addHeader("X-Tenant-Id", "foo")
      servletRequest.addHeader("X-Tenant-Id", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ProjectID\" : [  \"foo\"  ,\"bar\"  ]")
    }
    it("should extract and log multiple project id header values") {
      // given:
      servletRequest.addHeader("X-Project-Id", "foo")
      servletRequest.addHeader("X-Project-Id", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"ProjectID\" : [  \"foo\"  ,\"bar\"  ]")
    }
    it("should extract and log the request roles header") {
      // given:
      servletRequest.addHeader("X-Roles", "foo")
      servletRequest.addHeader("X-Roles", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Roles\" : [  \"foo\"  ,\"bar\"  ]")
    }
    it("should extract and log the request user agent") {
      // given:
      servletRequest.addHeader("User-Agent", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"UserAgent\" : \"foo\"")
    }
    it("should extract and log the response code") {
      // given:
      servletResponse.setStatus(418)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Code\" : 418")
    }
    it("should extract and log the response message") {
      // given:
      servletResponse.setStatus(418, "I'm a teapot")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Message\" : \"I_AM_A_TEAPOT\"")
    }
    it("should extract and log the response message of an invalid response code") {
      // given:
      servletResponse.setStatus(FilterDirector.SC_UNSUPPORTED_RESPONSE_CODE, "Unsupported Response Code")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Message\" : \"UNKNOWN\"")
    }
    ignore("should extract and log the response body") {
      //given:
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

  describe("the configurationUpdated method") {
    it("should leave the template alone when crush is false") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(false)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(418, "I'm a teapot")

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Line One\n Line Two")
    }

    it("should strip the template alone when crush is true") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(true)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(418, "I'm a teapot")

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Line One Line Two")
    }

    it("should strip the leading space of the template") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(false)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(418, "I'm a teapot")

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should not include ("\nLine One\n Line Two")
    }
  }
}
