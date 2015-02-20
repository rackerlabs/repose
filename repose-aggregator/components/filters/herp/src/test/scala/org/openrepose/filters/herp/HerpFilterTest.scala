package org.openrepose.filters.herp

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.filters.herp.config.{FilterOut, HerpConfig, Match, Template}
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.springframework.http.HttpStatus._
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HerpFilterTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfter with Matchers {

  var herpFilter: HerpFilter = _
  var herpConfig: HerpConfig = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var listAppenderPre: ListAppender = _
  var listAppenderPost: ListAppender = _

  override def beforeAll() {
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
      "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")
  }

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppenderPre = ctx.getConfiguration.getAppender("highly-efficient-record-processor-pre-ListAppender").asInstanceOf[ListAppender].clear
    listAppenderPost = ctx.getConfiguration.getAppender("highly-efficient-record-processor-post-ListAppender").asInstanceOf[ListAppender].clear

    herpFilter = new HerpFilter(null, "cluster", "node")
    herpConfig = new HerpConfig
    servletRequest = new MockHttpServletRequest
    servletRequest.setMethod("GET")
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain
    herpConfig.setPreFilterLoggerName("highly-efficient-record-processor-pre-Logger")
    herpConfig.setPostFilterLoggerName("highly-efficient-record-processor-post-Logger")
    val templateText =
      """
         {
          "GUID" : "{{guid}}",
          "ServiceCode" : "{{serviceCode}}",
          "Region" : "{{region}}",
          "DataCenter" : "{{dataCenter}}",
          "Cluster" : "{{clusterId}}",
          "Node" : "{{nodeId}}",
          "RequestorIp" : "{{requestorIp}}",
          "Timestamp" : "{{timestamp}}",
          "CadfTimestamp" : "{{cadfTimestamp timestamp}}",
          "Request" : {
            "Method" : "{{requestMethod}}",
            "CadfMethod" : "{{cadfMethod requestMethod}}",
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
            "CadfOutcome" : "{{cadfOutcome responseCode}}",
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
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include regex "\"ServiceCode\" : \"\".*\"URL\" : \"\""
    }
    it("should log a guid") {
      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"DataCenter\" : \"some-data-center\"")
    }
    it("should log the parametered cluster") {
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Cluster\" : \"cluster\"")
    }
    it("should log the parametered node") {
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Node\" : \"node\"")
    }
    it("should extract and log the x-forwarded-for header over the remote address") {
      // given:
      servletRequest.addHeader("X-FORWARDED-FOR", "1.2.3.4")
      servletRequest.setRemoteAddr("4.3.2.1")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"RequestorIp\" : \"1.2.3.4\"")
    }
    it("should extract and log the remote address") {
      // given:
      servletRequest.setRemoteAddr("4.3.2.1")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"RequestorIp\" : \"4.3.2.1\"")
    }
    it("should expose the cadf timestamp") {
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"CadfTimestamp\" : \"")
    }
    it("should extract and log the request method") {
      // given:
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Method\" : \"POST\"")
    }
    it("should expose the cadf method") {
      // given:
      servletRequest.setMethod("POST")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"CadfMethod\" : \"update/post\"")
    }
    it("should extract and log the request url") {
      // given:
      servletRequest.setAttribute("http://openrepose.org/requestUrl", "http://foo.com")

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
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
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"UserAgent\" : \"foo\"")
    }
    it("should extract and log the response code") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Code\" : 418")
    }
    it("should expose the cadf outcome") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"CadfOutcome\" : \"failure\"")
    }
    it("should extract and log the response message") {
      // given:
      servletResponse.setStatus(I_AM_A_TEAPOT.value)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Message\" : \"I_AM_A_TEAPOT\"")
    }
    it("should extract and log the response message of an invalid response code") {
      // given:
      servletResponse.setStatus(FilterDirector.SC_UNSUPPORTED_RESPONSE_CODE)

      // when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      // then:
      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should include("\"Message\" : \"UNKNOWN\"")
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
          def logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          def logEventsPost = listAppenderPost.getEvents
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
          def logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          def logEventsPost = listAppenderPost.getEvents
          logEventsPost.size shouldBe condition._2
        }
        it("when there are fields with maps with String keys and String array values and the condition is a value.") {
          // given:
          val test = "---foo---"
          servletRequest.setAttribute("http://openrepose.org/queryParams", Map(
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
          def logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test)

          def logEventsPost = listAppenderPost.getEvents
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
          matcherOne.setRegex(condition._1._1)  // Conditionally matches
          matchersOne.add(matcherOne)
          val matcherTwo = new Match            // AND'd
          matcherTwo.setField("roles")
          matcherTwo.setRegex(".*BUZ.*")        // Always matches
          matchersOne.add(matcherTwo)
          filtersOut.add(filterOne)
          val filterTwo = new FilterOut         // OR'd
          val matchersTwo = filterTwo.getMatch
          val matcherThree = new Match
          matcherThree.setField("userName")
          matcherThree.setRegex(condition._1._2)// Never Matches
          matchersTwo.add(matcherThree)
          val matcherFour = new Match           // AND'd
          matcherFour.setField("roles")
          matcherFour.setRegex(".*BUZ.*")       // Always matches
          matchersTwo.add(matcherFour)
          filtersOut.add(filterTwo)

          // when:
          herpFilter.configurationUpdated(herpConfig)
          herpFilter.doFilter(servletRequest, servletResponse, filterChain)

          // then:
          def logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(testOne.toString)
          logEventsPre.get(0).getMessage.getFormattedMessage should include(testTwo.toString)

          def logEventsPost = listAppenderPost.getEvents
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
          def logEventsPre = listAppenderPre.getEvents
          logEventsPre.size shouldBe 1
          logEventsPre.get(0).getMessage.getFormattedMessage should include(test.toString)

          def logEventsPost = listAppenderPost.getEvents
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

      def logEvents = listAppenderPre.getEvents
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

      def logEvents = listAppenderPre.getEvents
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

      def logEvents = listAppenderPre.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage should not include "\nLine One\n Line Two"
    }
  }

  describe("cadf timestamp") {
    it("should convert as expected") {
      val timestampFormater = new CadfTimestamp
      timestampFormater(0, null) should equal ("1969-12-31T18:00:00-06:00")
    }
  }

  describe("cadf method") {
    val methodFormatter = new CadfMethod
    val methods: Map[String, String] = Map(
      "get"    -> "read/get",
      "head"   -> "read/head",
      "post"   -> "update/post",
      "put"    -> "update/put",
      "delete" -> "update/delete",
      "patch"  -> "update/patch"
    )
    methods.foreach { method =>
      it(s"should translate ${method._1} into ${method._2}") {
        methodFormatter(method._1, null) should equal (method._2)
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
        outcomeFormatter(outcome._1, null) should equal (outcome._2)
      }
    }
  }
}
