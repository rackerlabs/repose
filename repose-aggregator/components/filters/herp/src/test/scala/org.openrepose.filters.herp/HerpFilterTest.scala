package org.openrepose.filters.herp

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.openrepose.filters.herp.config.HerpConfig
import org.scalatest._
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

class HerpFilterTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfter with Matchers with PrivateMethodTester {

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

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    listAppender = ctx.getConfiguration.getAppender("highly-efficient-record-processor-ListAppender").asInstanceOf[ListAppender].clear
  }

  describe("The doFilter method") {
    it("should ...") {
      //given:
      val herpFilter = new HerpFilter()
      val herpConfig = new HerpConfig()
      val servletRequest = new MockHttpServletRequest()
      val servletResponse = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      herpConfig.setId("highly-efficient-record-processor-Logger")
      servletRequest.setRequestURI("http://www.example.com/derp/derp?herp=derp")
      servletRequest.addHeader("Accept", "application/xml")
      servletRequest.setMethod("GET")
      servletRequest.setRemoteHost("10.10.220.221")
      servletRequest.setLocalAddr("10.10.220.220")
      servletRequest.setLocalPort(12345)
      servletRequest.setServerPort(8080)
      servletRequest.addHeader("X-PP-User", "leUser") //Remote user is special for Repose...

      val responseBody = "HEY A BODY"
      servletResponse.setContentLength(responseBody.length)
      servletResponse.setStatus(200, "OK")
      servletResponse.addHeader("X-Derp-header", "lolwut")
      servletResponse.getWriter().write(responseBody)
      servletResponse.getWriter().flush()
      servletResponse.getWriter().close()

      //when:
      herpFilter.configurationUpdated(herpConfig)
      herpFilter.doFilter(servletRequest, servletResponse, filterChain)

      //then:
      def logEvents = listAppender.getEvents
      logEvents.size shouldBe 1
      logEvents.get(0).getMessage.getFormattedMessage shouldBe "This is a message from the HERP filter."
    }
  }
}
