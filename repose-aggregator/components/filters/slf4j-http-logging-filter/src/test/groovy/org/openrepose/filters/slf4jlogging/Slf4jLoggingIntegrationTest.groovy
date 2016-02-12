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
package org.openrepose.filters.slf4jlogging

import com.mockrunner.mock.web.MockFilterChain
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@Ignore
class Slf4jLoggingIntegrationTest extends Specification {
    ListAppender app

    @Shared
    Slf4jHttpLoggingFilter filter

    def setupSpec() {
        //NOTE This dies a horrible death if you try to stick it in an unroll. Possibly a side effect of how brittle it is
        filter = Slf4jLoggingFilterTestUtil.configureFilter([
                //Configure a logger with all the things so I can verify all the things we claim to support
                Slf4jLoggingFilterTestUtil.logConfig("Logger0", "%a\t%A\t%b\t%B\t%h\t%m\t%p\t%q\t%t\t%s\t%u\t%U\t%{Accept}i\t%r\t%H\t%{X-Derp-header}o\t%D\t%T\t%M")
        ])
    }

    def setup() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear()
    }

    def "The SLF4j logging filter logs to the named logger"() {
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
        response.setStatus(200, "OK")
        response.addHeader("X-Derp-header", "lolwut")
        response.getWriter().print(responseBody)
        response.getWriter().flush()
        response.getWriter().close() //I think this should shove the body in there

        when:
        filter.doFilter(request, response, chain)

        then:
        chain.getRequestList().size() == 1

        app.getEvents().size() == 1

        def splitLog = app.getEvents().first().getMessage().getFormattedMessage().split("\t").toList()

        //splitLog.size() == 19

        splitLog[0] == "127.0.0.1" //REMOTE_ADDRESS
        splitLog[1] == "10.10.220.220" //LOCAL_ADDRESS
        splitLog[2] == "-" //REPSONSE_CLF_BYTES -- Should be contentLength, but it's not, maybe because mocking
        splitLog[3] == "0" //Should be response Bytes, 10, but it's not
        splitLog[4] == "10.10.220.221" //configured remote host
        splitLog[5] == "GET" //making a get
        splitLog[6] == "12345" //CANONICAL port
        splitLog[7] == "?herp=derp" //QueryString
        //splitLog[8] == //Should be within a certain amount of time from nowish say a minute?
        splitLog[9] == "200" //Status code
        splitLog[10] == "leUser" //Remote user
        splitLog[11] == "http://www.example.com/derp/derp?herp=derp" //URL Requested
        splitLog[12] == "application/xml" //Request accept header
        splitLog[13] == "GET http://www.example.com/derp/derp?herp=derp HTTP/1.1" //Request line
        splitLog[14] == "HTTP/1.1" //Request Protocol
        splitLog[15] == "lolwut"

        //The rest seem to be somewhat magical, or it's my argument parsing insanity
    }
}
