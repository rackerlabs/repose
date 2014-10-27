package org.openrepose.filters.slf4jlogging

import com.mockrunner.mock.web.MockFilterChain
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.junit.InitialLoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

class Slf4jMultipleLoggersTest extends Specification {
    private static final String CONFIG = "classpath:log4j2-Slf4jMultipleLoggersTest.xml";

    @Rule
    InitialLoggerContext init = new InitialLoggerContext(CONFIG)
    ListAppender app1;
    ListAppender app2;
    ListAppender app3;

    @Shared
    Slf4jHttpLoggingFilter filter

    def setupSpec() {
        filter = Slf4jLoggingFilterTestUtil.configureFilter([
                //Configure a logger with all the things so I can verify all the things we claim to support
                Slf4jLoggingFilterTestUtil.logConfig("Logger1", "%r"),
                Slf4jLoggingFilterTestUtil.logConfig("Logger2", "%m"),
                Slf4jLoggingFilterTestUtil.logConfig("Logger3", "%a")
        ])
    }

    def setup() {
        app1 = init.getListAppender("List1").clear();
        app2 = init.getListAppender("List2").clear();
        app3 = init.getListAppender("List3").clear();
    }

    def "The SLF4j logging filter logs to the named loggers"(){
        given:
        MockFilterChain chain = new MockFilterChain()
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()

        //Will need to set up the request and response to verify the log line
        request.setRequestURI("http://www.example.com/derp/derp?herp=derp")
        request.setRequestURL("http://www.example.com/derp/derp?herp=derp")
        request.addHeader("Accept", "application/xml")
        request.setQueryString("?herp=derp")
        request.setMethod("GET")
        request.setRemoteHost("10.10.220.221")
        request.setLocalAddr("10.10.220.220")
        request.setLocalPort(12345)
        request.setServerPort(8080)
        request.addHeader("X-PP-User", "leUser") //Remote user is special for Repose...


        def responseBody = "HEY A BODY"
        response.setContentLength(10)// size of responseBody .. but no
        response.setStatus(200,"OK")
        response.addHeader("X-Derp-header", "lolwut")
        response.getWriter().print(responseBody)
        response.getWriter().flush()
        response.getWriter().close() //I think this should shove the body in there

        when:
        filter.doFilter(request, response, chain)
        List<HttpServletRequest> requestList = chain.getRequestList();
        List<LogEvent> events1 = app1.getEvents();
        List<LogEvent> events2 = app2.getEvents();
        List<LogEvent> events3 = app3.getEvents();

        then:
        requestList.size() == 1

        events1.size() == 1
        events1.first().getMessage().getFormattedMessage().equals("GET http://www.example.com/derp/derp?herp=derp HTTP/1.1");

        events2.size() == 1
        events2.first().getMessage().getFormattedMessage().equals("GET");

        events3.size() == 1
        events3.first().getMessage().getFormattedMessage().equals("127.0.0.1");
    }
}
