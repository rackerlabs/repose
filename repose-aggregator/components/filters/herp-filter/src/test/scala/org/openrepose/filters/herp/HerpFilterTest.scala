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
package org.openrepose.filters.herp

import java.util.{TimeZone, UUID}

import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.CommonRequestAttributes.QUERY_PARAMS
import org.openrepose.filters.herp.config.{FilterOut, HerpConfig, Match, Template}
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.springframework.http.HttpStatus._
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HerpFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var herpFilter: HerpFilter = _
  var herpConfig: HerpConfig = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  var listAppenderPre: ListAppender = _
  var listAppenderPost: ListAppender = _

  override def beforeEach(): Unit = {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppenderPre = ctx.getConfiguration.getAppender("highly-efficient-record-processor-pre-ListAppender").asInstanceOf[ListAppender].clear
    listAppenderPost = ctx.getConfiguration.getAppender("highly-efficient-record-processor-post-ListAppender").asInstanceOf[ListAppender].clear

    herpFilter = new HerpFilter(null, "node")
    herpConfig = new HerpConfig
    servletRequest = new MockHttpServletRequest
    servletRequest.setMethod("GET")
    servletResponse = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    herpConfig.setPreFilterLoggerName("highly-efficient-record-processor-pre-Logger")
    herpConfig.setPostFilterLoggerName("highly-efficient-record-processor-post-Logger")
    val templateText =
      """{
        |  "GUID" : "{{guid}}",
        |  "ServiceCode" : "{{serviceCode}}",
        |  "Region" : "{{region}}",
        |  "DataCenter" : "{{dataCenter}}",
        |  "Node" : "{{nodeId}}",
        |  "RequestorIp" : "{{requestorIp}}",
        |  "Timestamp" : "{{timestamp}}",
        |  "CadfTimestamp" : "{{cadfTimestamp timestamp}}",
        |  "Request" : {
        |    "Method" : "{{requestMethod}}",
        |    "MethodLabel" : "{{methodLabel}}",
        |    "CadfMethod" : "{{cadfMethod requestMethod}}",
        |    "URL" : "{{requestURL}}",
        |    "TargetHost" : "{{targetHost}}",
        |    "QueryString" : "{{requestQueryString}}",
        |    "Parameters" : { {{#each parameters}}{{#if @index}},{{/if}}"{{key}}" : [{{#each value}}{{#if @index}},{{/if}}"{{{.}}}"{{/each}}]{{/each}}
        |                   },
        |    "UserName" : "{{userName}}",
        |    "ImpersonatorName" : "{{impersonatorName}}",
        |    "DefaultProjectID" : "{{defaultProjectId}}",
        |    "ProjectID" : [
        |                    {{#each projectID}}
        |                    {{#if @index}},{{/if}}"{{.}}"
        |                    {{/each}}
        |                  ],
        |    "Roles" : [
        |                {{#each roles}}
        |                {{#if @index}},{{/if}}"{{.}}"
        |                {{/each}}
        |              ],
        |    "UserAgent" : "{{userAgent}}"
        |  },
        |  "Response" : {
        |    "Code" : {{responseCode}},
        |    "CadfOutcome" : "{{cadfOutcome responseCode}}",
        |    "Message" : "{{responseMessage}}"
        |  }
        |}
        |""".stripMargin
    val template = new Template
    template.setValue(templateText)
    template.setCrush(true)
    herpConfig.setTemplate(template)
  }

  describe("the doFilter method") {

    /**
      * Grabs the wrapped response that was passed in to the filterChain and adds the headers.
      */
    def addResponseHeaders(headers: List[(String, String)]): Unit = {
      val responseCaptor = ArgumentCaptor.forClass(classOf[HttpServletResponse])
      doAnswer(new Answer[Unit]() {
        def answer(invocation: InvocationOnMock): Unit = {
          val response = responseCaptor.getValue
          headers.foreach { case (hdrName, hdrValue) =>
            response.addHeader(hdrName, hdrValue)
          }
        }
      }).when(filterChain).doFilter(any(classOf[HttpServletRequest]), responseCaptor.capture())
    }

    it("should be empty if field data is not present") {
      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex """"ServiceCode" : "".*"URL" : """""
    }
    it("should log a guid") {
      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex """"GUID" : "NO_TRANSACTION_ID:.+""""
    }
    it("should log a guid given a tracing header") {
      // given:
      val traceId = UUID.randomUUID.toString
      servletRequest.addHeader(CommonHttpHeader.TRACE_GUID, traceId)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex s""""GUID" : "$traceId:.+""""
    }
    it("should log the configured service code") {
      // given:
      herpConfig.setServiceCode("some-service")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ServiceCode" : "some-service"""")
    }
    it("should log the configured region") {
      // given:
      herpConfig.setRegion("some-region")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Region" : "some-region"""")
    }
    it("should log the configured data center") {
      // given:
      herpConfig.setDataCenter("some-data-center")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""DataCenter" : "some-data-center"""")
    }
    it("should log the parametered node") {
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Node" : "node"""")
    }
    it("should extract and log the x-forwarded-for header over the remote address") {
      // given:
      servletRequest.addHeader("X-FORWARDED-FOR", "1.2.3.4")
      servletRequest.setRemoteAddr("4.3.2.1")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""RequestorIp" : "1.2.3.4"""")
    }
    it("should extract and log the remote address") {
      // given:
      servletRequest.setRemoteAddr("4.3.2.1")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""RequestorIp" : "4.3.2.1"""")
    }
    it("should expose the cadf timestamp") {
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""CadfTimestamp" : """")
    }
    it("should extract and log the request method") {
      // given:
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Method" : "POST"""")
    }
    it("should expose the cadf method") {
      // given:
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""CadfMethod" : "update/post"""")
    }
    it("should extract and log the request url") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/requestUrl", "http://foo.com")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""URL" : "http://foo.com"""")
    }
    it("should extract and log the unaltered request query string") {
      // given:
      servletRequest.setQueryString("a=b&c=d%20e")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""QueryString" : "a=b&amp;c=d%20e"""")
    }
    it("should extract and log the target host") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/requestUrl", "http://foo.com")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""TargetHost" : "foo.com"""")
    }
    it("should extract and log the request parameters") {
      // given:
      servletRequest.setAttribute(QUERY_PARAMS, Map("foo" -> Array("bar", "baz")).asJava)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Parameters" : { "foo" : ["bar","baz"] }""")
    }
    it("should extract and log the request user name header") {
      // given:
      servletRequest.addHeader("X-User-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""UserName" : "foo"""")
    }
    it("should extract and log the response user name header when the header is not available in the request") {
      // given:
      addResponseHeaders(List(("X-User-Name", "bar")))

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""UserName" : "bar"""")
    }
    List(
      ("", "responseBar", "responseBar"),
      (";q=1.0", "responseBar", "responseBar"),
      ("requestFoo", "", "requestFoo"),
      ("requestFoo", "responseBar", "requestFoo"),
      ("requestFoo;q=1.0", "responseBar", "requestFoo"),
      ("requestFoo;q=1.0", "responseBar;q=1.0", "requestFoo"),
      ("requestFoo;q=0.2", "responseBar;q=0.9", "requestFoo"),
      ("requestFoo;pie=good", "responseBar;q=0.9", "requestFoo"),
      ("requestFoo;pie=good;q=0.4", "responseBar;q=0.8", "requestFoo"),
      ("requestFoo;pie=good;q=0.6;foo=baz", "responseBar;q=0.7", "requestFoo"),
      ("", "responseBar;q=1.0", "responseBar"),
      ("", "responseBar;q=0.3", "responseBar"),
      ("", "responseBar;pie=good", "responseBar"),
      ("", "responseBar;pie=good;q=0.7", "responseBar"),
      ("", "responseBar;pie=good;q=0.7;foo=bar", "responseBar")
    ) foreach { case (requestHeader, responseHeader, expectedUsername) =>
      it(s"should extract and log the user name header '$expectedUsername' when request header is '$requestHeader' and response header is '$responseHeader'") {
        // given:
        servletRequest.addHeader("X-User-Name", requestHeader)
        addResponseHeaders(List(("X-User-Name", responseHeader)))

        // when:
        herpFilter.configurationUpdated(herpConfig)
        herpFilter.doFilter(servletRequest, servletResponse, filterChain)

        // then:
        val logEvents = listAppenderPre.getEvents
        logEvents.size shouldBe 1
        logEvents.get(0).getMessage.getFormattedMessage should include(s""""UserName" : "$expectedUsername"""")
      }
    }
    it("should extract and log the request impersonator name header") {
      // given:
      servletRequest.addHeader("X-Impersonator-Name", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ImpersonatorName" : "foo"""")
    }
    it("should extract and log the request tenant id header") {
      // given:
      servletRequest.addHeader("X-Tenant-Id", "foo")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ]""")
    }
    it("should extract and log the request tenant id header from Response if not available from Request") {
      // given:
      addResponseHeaders(List(("X-Tenant-Id", "foo")))

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ]""")
    }
    it("should extract and log the default request tenant id header") {
      // given:
      servletRequest.addHeader("X-Tenant-Id", "foo;q=0.5")
      servletRequest.addHeader("X-Tenant-Id", "bar;q=1.0")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""DefaultProjectID" : "bar"""")
    }
    it("should extract and log the default request tenant id header from Response if not available from Request") {
      // given:
      addResponseHeaders(List(("X-Tenant-Id", "foo;q=0.5"), ("X-Tenant-Id", "bar;q=1.0")))

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""DefaultProjectID" : "bar"""")
    }

    val hdrNames = List("X-Tenant-Id", "X-Project-Id")
    val reqValues = List(List("reqFoo;q=0.5", "reqBar;q=1.0"), List("reqFoo;q=0.5,reqBar;q=1.0"))
    val resValues = List(List("resFoo;q=0.5", "resBar;q=1.0"), List("resFoo;q=0.5,resBar;q=1.0"))
    val headers = for (reqHdr <- hdrNames; resHdr <- hdrNames) yield (reqHdr, resHdr)
    val values = for (reqVal <- reqValues; resVal <- resValues) yield (reqVal, resVal)
    headers foreach { case (reqHdr, resHdr) =>
      values foreach { case (reqVal, resVal) =>
        it(s"should log the default tenant id header from the Request even if it is available on the Response (REQ=$reqHdr:$reqVal)(RES=$resHdr:$resVal)") {
          // given:
          reqVal foreach { value =>
            servletRequest.addHeader(reqHdr, value)
          }
          addResponseHeaders(
            resVal.foldLeft(List[(String, String)]()) { (accumulator, current) =>
              accumulator.::(resHdr, current)
            }
          )

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEvents = listAppenderPre.getEvents
          logEvents.size shouldBe 1
          logEvents.get(0).getMessage.getFormattedMessage should include(""""DefaultProjectID" : "reqBar"""")
        }
      }
    }

    it("should extract and log multiple tenant id header values") {
      // given:
      servletRequest.addHeader("X-Tenant-Id", "foo")
      servletRequest.addHeader("X-Tenant-Id", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ,"bar"  ]""")
    }
    it("should extract and log multiple tenant id header values from Response if not available from Request") {
      // given:
      addResponseHeaders(List(("X-Tenant-Id", "foo"), ("X-Tenant-Id", "bar")))

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ,"bar"  ]""")
    }
    it("should extract and log multiple project id header values") {
      // given:
      servletRequest.addHeader("X-Project-Id", "foo")
      servletRequest.addHeader("X-Project-Id", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ,"bar"  ]""")
    }
    it("should extract and log multiple project id header values from Response if not available from Request") {
      // given:
      addResponseHeaders(List(("X-Project-Id", "foo"), ("X-Project-Id", "bar")))

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""ProjectID" : [  "foo"  ,"bar"  ]""")
    }
    it("should extract and log the request roles header") {
      // given:
      servletRequest.addHeader("X-Roles", "foo")
      servletRequest.addHeader("X-Roles", "bar")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Roles" : [  "foo"  ,"bar"  ]""")
    }
    it("should extract and log the entire request user agent") {
      // given:
      servletRequest.addHeader("User-Agent", "Repozilla/5000.0 (Bartlett; Leg Bart OS L 50_0_0) PearWebKit/987.65 (RHTML, like Skink) Gold/98.7.6543.210 Journey/123.45")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""UserAgent" : "Repozilla/5000.0 (Bartlett; Leg Bart OS L 50_0_0) PearWebKit/987.65 (RHTML, like Skink) Gold/98.7.6543.210 Journey/123.45"""")
    }
    it("should extract and log the response code") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Code" : 418""")
    }
    it("should expose the cadf outcome") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""CadfOutcome" : "failure"""")
    }
    it("should extract and log the response message") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Message" : "I_AM_A_TEAPOT"""")
    }
    it("should extract and log the response message of an invalid response code") {
      // given:
      servletResponse.setStatus(-1)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""Message" : "UNKNOWN"""")
    }
    it("should extract and log the header corresponding to the label of a method") {
      // given:
      servletRequest.addHeader("X-METHOD-LABEL", "getServers")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include(""""MethodLabel" : "getServers"""")
    }
  }

  describe("the doFilter method with a filtering config,") {
    val conditions: Map[String, Int] = Map(
      // Regex     | Log Events
      ".*[Ff]oo.*" -> 0,
      ".*[Bb]ar.*" -> 1)
    conditions.foreach { condition =>
      describe(s"if the regex is ${condition._1}, then the total unfiltered events should be ${condition._2},") {
        it("when the field has a String value.") {
          // given:
          val test = "---foo---"
          servletRequest.addHeader("X-User-Name", test)
          val matcher = new Match
          matcher.setField("userName")
          matcher.setRegex(condition._1)
          val filterOut = new FilterOut
          filterOut.getMatch.add(matcher)
          herpConfig.getFilterOut.add(filterOut)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          val logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
        it("when there are fields with String array values.") {
          // given:
          val test = "---foo---"
          servletRequest.addHeader("X-Roles", test)
          val matcher = new Match
          matcher.setField("roles")
          matcher.setRegex(condition._1)
          val filterOut = new FilterOut
          filterOut.getMatch.add(matcher)
          herpConfig.getFilterOut.add(filterOut)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          val logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
        it("when there are fields with maps with String keys and String array values and the condition is a value.") {
          // given:
          val test = "---foo---"
          servletRequest.setAttribute(QUERY_PARAMS, Map(
            "---bar---" -> Array("A", "B", "C"),
            "---buz---" -> Array("1", "2", test)).asJava
          )

          val matcher = new Match
          matcher.setField("parameters.---buz---")
          matcher.setRegex(condition._1)
          val filterOut = new FilterOut
          filterOut.getMatch.add(matcher)
          herpConfig.getFilterOut.add(filterOut)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          val logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
      }
    }
  }

  describe("the doFilter method with a filtering config,") {
    val conditions: Map[(String, String), Int] = Map(
      // Regex     | Log Events
      (".*[Ff]oo.*", "NO-MATCH") -> 0,
      ("NO-MATCH", ".*[Ff]oo.*") -> 0,
      (".*[Bb]ar.*", "NO-MATCH") -> 1,
      ("NO-MATCH", ".*[Bb]ar.*") -> 1)
    conditions.foreach { condition =>
      describe(s"if the regex is ${condition._1}, then the total unfiltered events should be ${condition._2},") {
        it("when the matches of a filterOut are AND'd and the filterOut's are OR'd.") {
          // given:
          val testOne = "---foo---"
          val testTwo = "---BUZ---"
          servletRequest.addHeader("X-User-Name", testOne)
          servletRequest.addHeader("X-Roles", testTwo)
          val filtersOut = herpConfig.getFilterOut
          val filterOne = new FilterOut
          val matchersOne = filterOne.getMatch
          val matcherOne = new Match
          matcherOne.setField("userName")
          matcherOne.setRegex(condition._1._1) // Conditionally matches
          matchersOne.add(matcherOne)
          val matcherTwo = new Match // AND'd
          matcherTwo.setField("roles")
          matcherTwo.setRegex(".*BUZ.*") // Always matches
          matchersOne.add(matcherTwo)
          filtersOut.add(filterOne)
          val filterTwo = new FilterOut // OR'd
          val matchersTwo = filterTwo.getMatch
          val matcherThree = new Match
          matcherThree.setField("userName")
          matcherThree.setRegex(condition._1._2) // Never Matches
          matchersTwo.add(matcherThree)
          val matcherFour = new Match // AND'd
          matcherFour.setField("roles")
          matcherFour.setRegex(".*BUZ.*") // Always matches
          matchersTwo.add(matcherFour)
          filtersOut.add(filterTwo)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(testOne.toString)
          logEventsPre.get(0).getMessage.getFormattedMessage should include(testTwo.toString)

          val logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
      }
    }
  }

  describe("the doFilter method with a filtering config,") {
    val conditions: Map[String, Int] = Map(
      // Regex     | Log Events
      "4[01]8" -> 0,
      "4[23]8" -> 1)
    conditions.foreach { condition =>
      describe(s"if the regex is ${condition._1}, then the total unfiltered events should be ${condition._2},") {
        it("when there are fields with Integer values.") {
          // given:
          val test = I_AM_A_TEAPOT.value
          servletResponse.setStatus(test)
          val matcher = new Match
          matcher.setField("responseCode")
          matcher.setRegex(condition._1)
          val filterOut = new FilterOut
          filterOut.getMatch.add(matcher)
          herpConfig.getFilterOut.add(filterOut)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          val logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test.toString)

          val logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
      }
    }
  }

  describe("the configurationUpdated method") {
    it("should leave the template alone when crush is false") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(false)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Line One\n Line Two")
    }

    it("should strip the template alone when crush is true") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(true)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Line One Line Two")
    }

    it("should strip the leading space of the template") {
      val template: Template = new Template
      template.setValue("\n Line One\n Line Two")
      template.setCrush(false)
      herpConfig.setTemplate(template)
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      val logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should not include "\nLine One\n Line Two"
    }
  }

  describe("cadf timestamp") {
    val timestampFormater = new CadfTimestamp
    val timeZones: Map[TimeZone, String] = Map(
      TimeZone.getTimeZone("Etc/GMT-1") -> "1970-01-01T01:00:00.000+01:00",
      TimeZone.getTimeZone("GMT") -> "1970-01-01T00:00:00.000+00:00",
      TimeZone.getTimeZone("GMT0") -> "1970-01-01T00:00:00.000+00:00",
      TimeZone.getTimeZone("UTC") -> "1970-01-01T00:00:00.000+00:00",
      TimeZone.getTimeZone("US/Eastern") -> "1969-12-31T19:00:00.000-05:00",
      TimeZone.getTimeZone("US/Central") -> "1969-12-31T18:00:00.000-06:00",
      TimeZone.getTimeZone("US/Mountain") -> "1969-12-31T17:00:00.000-07:00",
      TimeZone.getTimeZone("US/Pacific") -> "1969-12-31T16:00:00.000-08:00"
    )
    timeZones.foreach { timeZone =>
      it(s"should convert as expected in ${timeZone._1.getDisplayName} (${timeZone._1.getID})") {
        TimeZone.setDefault(timeZone._1)
        timestampFormater(0, null) shouldEqual timeZone._2
      }
    }
  }

  describe("cadf method") {
    val methodFormatter = new CadfMethod
    val methods: Map[String, String] = Map(
      "get" -> "read/get",
      "head" -> "read/head",
      "options" -> "read/options",
      "post" -> "update/post",
      "put" -> "update/put",
      "delete" -> "update/delete",
      "patch" -> "update/patch",
      "foo" -> "unknown/foo"
    )
    methods.foreach { method =>
      it(s"should translate ${method._1} into ${method._2}") {
        methodFormatter(method._1, null) shouldEqual method._2
      }
    }
  }

  describe("cadf outcome") {
    val outcomeFormatter = new CadfOutcome
    val outcomes: Map[Int, String] = Map(
      200 -> "success",
      201 -> "success",
      204 -> "success",
      301 -> "failure",
      301 -> "failure",
      400 -> "failure",
      404 -> "failure",
      412 -> "failure",
      429 -> "failure",
      500 -> "failure",
      503 -> "failure"
    )
    outcomes.foreach { outcome =>
      it(s"should translate status ${outcome._1} into ${outcome._2}") {
        outcomeFormatter(outcome._1, null) shouldEqual outcome._2
      }
    }
  }
}
