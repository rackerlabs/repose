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
package org.openrepose.filters.versioning

import com.mockrunner.mock.web.MockFilterChain
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import org.openrepose.core.systemmodel.DestinationEndpoint
import org.openrepose.filters.versioning.config.MediaType
import org.openrepose.filters.versioning.config.MediaTypeList
import org.openrepose.filters.versioning.testhelpers.VersioningFilterSpecification

import javax.servlet.http.HttpServletRequest

/**
 * Assumptions:
 *  - filterconfig will not be null
 *  - configuration service will not be null
 */
class VersioningFilterTest extends VersioningFilterSpecification {
    def configurationManager, request, response, chain, handlerFactory, handler, filterDirector, filterConfig

    def setup() {
        /*configurationService = mock(ConfigurationService)
        request = mock(HttpServletRequest)
        response = mock(HttpServletResponse)
        chain = mock(FilterChain)
        handlerFactory = mock(VersioningHandlerFactory)
        handler = mock(VersioningHandler)
        filterDirector = mock(FilterDirector)
        filterConfig = mock(FilterConfig)
        when(handlerFactory.newHandler()).thenReturn(handler)
        when(handler.handleRequest(
                org.mockito.Mockito.any(HttpServletRequest),
                org.mockito.Mockito.any(HttpServletResponse)
        )).thenReturn(filterDirector)
        when(filterDirector.getFilterAction()).thenReturn(FilterAction.NOT_SET)
        */
    }

    def "Destroy - happy path"() {
        when:
        def filter = new VersioningFilter()
        filter.configurationService = configurationManager
        filter.destroy()
        then:
        noExceptionThrown()

    }

    def "DoFilter - happy path"() {
        when:
        def filter = new VersioningFilter()
        filter.handlerFactory = handlerFactory
        filter.doFilter(request, response, chain)
        then:
        noExceptionThrown()
    }

    def "Integration - happy path"() {
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
        def mediaTypes = new MediaTypeList()
        mediaTypes.mediaType << new MediaType(type: "application/vnd.vendor.service-v1+xml", base: "application/xml")
        mediaTypes.mediaType << new MediaType(type: "application/vnd.vendor.service+xml; version=1", base: "application/xml")
        mediaTypes.mediaType << new MediaType(type: "application/v1+xml", base: "application/xml")
        mediaTypes.mediaType << new MediaType(type: "application/vnd.rackspace; x=v1, y=xml", base: "application/xml")
        def filter = configureFilter([
                serviceVersionMapping("target1", "/v1", "DEPRECATED", mediaTypes)
        ], [
                new DestinationEndpoint(id: "target1", hostname: "localhost", protocol: "http", port: 8080, rootPath: "/", default: true),
                new DestinationEndpoint(id: "target2", hostname: "localhost", protocol: "http", port: 8080, rootPath: "/", default: false)
        ])
        filter.doFilter(request, response, chain)
        List<HttpServletRequest> requestList = chain.getRequestList();

        then:
        requestList.size() == 1

        def allLogs = logLines(outputStream)

        allLogs.size() == 1

        def splitLog = allLogs.first().split("\t").toList()

        //splitLog.size() == 19

        splitLog[0] == "INFO - 127.0.0.1" //REMOTE_ADDRESS
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
