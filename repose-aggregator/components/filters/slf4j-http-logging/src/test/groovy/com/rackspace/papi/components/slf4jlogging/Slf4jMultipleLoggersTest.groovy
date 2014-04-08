package com.rackspace.papi.components.slf4jlogging

import com.mockrunner.mock.web.MockFilterChain
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import spock.lang.Shared

import javax.servlet.http.HttpServletRequest

class Slf4jMultipleLoggersTest extends Slf4jLoggingFilterSpecification {

    @Shared
    Slf4jHttpLoggingFilter filter

    def setupSpec() {
        filter = configureFilter([
                //Configure a logger with all the things so I can verify all the things we claim to support
                logConfig("firstLogger", "%r"),
                logConfig("secondLogger", "%m"),
                logConfig("thirdLogger", "%a")
        ])
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

        def os1 = prepLoggerOutputStream("firstLogger")
        def os2 = prepLoggerOutputStream("secondLogger")
        def os3 = prepLoggerOutputStream("thirdLogger")



        when:
        filter.doFilter(request, response, chain)
        List<HttpServletRequest> requestList = chain.getRequestList();

        then:
        requestList.size() == 1

        def stream1 = logLines(os1)
        stream1.size() == 1
        stream1.first() == "INFO - GET http://www.example.com/derp/derp?herp=derp HTTP/1.1"

        def stream2 = logLines(os2)
        stream2.size() == 1
        stream2.first() == "INFO - GET"

        def stream3 = logLines(os3)
        stream3.size() == 1
        stream3.first() == "INFO - 127.0.0.1"

    }

}
