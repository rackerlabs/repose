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
package org.openrepose.powerfilter

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.mock

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 4/3/15
 * Time: 10:30 AM
 */
class PowerFilterChainTest extends Specification {

    @Unroll
    def "startFilterChain correctly handles header #headerName"() {
        given:
        HttpServletRequest resultRequest = null
        def powerFilterChain = new PowerFilterChain([], null, null, null) {
            @Override
            void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                resultRequest = (HttpServletRequest) servletRequest
            }
        }
        def mockRequest = new MockHttpServletRequest()
        headerValues.each {
            mockRequest.addHeader(headerName, it)
        }

        when:
        powerFilterChain.startFilterChain(mockRequest, null)

        then:
        getEnumerationLength(resultRequest.getHeaders(headerName)) == expectedNumber

        where:
        headerName            | headerValues                   | expectedNumber
        "accept"              | ["foo", "bar,baz"]             | 3
        "accept-charset"      | ["foo", "bar,baz"]             | 3
        "accept-language"     | ["foo", "bar,baz"]             | 3
        "allow"               | ["foo", "bar,baz"]             | 3
        "cache-control"       | ["foo", "bar,baz"]             | 3
        "connection"          | ["foo", "bar,baz"]             | 3
        "content-encoding"    | ["foo", "bar,baz"]             | 3
        "content-language"    | ["foo", "bar,baz"]             | 3
        "expect"              | ["foo", "bar,baz"]             | 3
        "pragma"              | ["foo", "bar,baz"]             | 3
        "proxy-authenticate"  | ["foo", "bar,baz"]             | 3
        "te"                  | ["foo", "bar,baz"]             | 3
        "trailer"             | ["foo", "bar,baz"]             | 3
        "transfer-encoding"   | ["foo", "bar,baz"]             | 3
        "upgrade"             | ["foo", "bar,baz"]             | 3
        "warning"             | ["foo", "bar,baz"]             | 3
        "accept-encoding"     | ["foo", "bar,baz"]             | 3
        "X-PP-Next-Route"     | ["foo", "bar,baz"]             | 3
        "X-PP-User"           | ["foo", "bar,baz"]             | 3
        "X-PP-Groups"         | ["foo", "bar,baz"]             | 3
        "x-catalog"           | ["foo", "bar,baz"]             | 3
        "X-Authorization"     | ["foo;q=0.1", "bar,baz"]       | 3
        "X-Identity-Status"   | ["foo", "bar,baz"]             | 3
        "X-User-Name"         | ["foo", "bar,baz"]             | 3
        "X-User-Id"           | ["foo", "bar;q=0.2,baz;q=0.3"] | 3
        "X-Tenant-Name"       | ["foo", "bar,baz"]             | 3
        "X-Tenant-Id"         | ["foo", "bar,baz"]             | 3
        "X-Roles"             | ["foo", "bar,baz"]             | 3
        "X-Impersonator-Id"   | ["foo", "bar,baz"]             | 3
        "X-Impersonator-Name" | ["foo", "bar,baz"]             | 3
        "X-Default-Region"    | ["foo", "bar,baz"]             | 3
        "x-token-expires"     | ["foo", "bar,baz"]             | 3
        "X-CONTACT-ID"        | ["foo", "bar,baz"]             | 3
        "X-TTL"               | ["foo", "bar,baz"]             | 3
        "whatever"            | ["foo", "bar,baz"]             | 2
    }

    @Unroll
    def "doRouting correctly handles header #headerName"() {
        given:
        def name = headerName
        def values = headerValues
        def router = new PowerFilterRouter() {
            @Override
            void route(MutableHttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException {
                values.each {
                    servletResponse.addHeader(name, it)
                }
            }
        }
        def powerFilterChain = new PowerFilterChain([], mock(FilterChain), router, null)
        def mockRequest = new MockHttpServletRequest()
        //NOTE: The PowerFilterChain only works if the initial response it is given is one of our
        // MutableHttpServletResponses, because it never writes changes down into the response
        def response = MutableHttpServletResponse.wrap(mockRequest, new MockHttpServletResponse())
        response.setStatus(200)

        when:
        powerFilterChain.doRouting(MutableHttpServletRequest.wrap(mockRequest), response)

        then:
        response.getHeaders(headerName).size() == expectedNumber

        where:
        headerName            | headerValues                   | expectedNumber
        "accept"              | ["foo", "bar,baz"]             | 3
        "accept"              | ["", "bar,baz"]                | 2
        "accept"              | ["foo", ",baz"]                | 2
        "accept-charset"      | ["foo", "bar,baz"]             | 3
        "accept-language"     | ["foo", "bar,baz"]             | 3
        "allow"               | ["foo", "bar,baz"]             | 3
        "cache-control"       | ["foo", "bar,baz"]             | 3
        "connection"          | ["foo", "bar,baz"]             | 3
        "content-encoding"    | ["foo", "bar,baz"]             | 3
        "content-language"    | ["foo", "bar,baz"]             | 3
        "expect"              | ["foo", "bar,baz"]             | 3
        "pragma"              | ["foo", "bar,baz"]             | 3
        "proxy-authenticate"  | ["foo", "bar,baz"]             | 3
        "te"                  | ["foo", "bar,baz"]             | 3
        "trailer"             | ["foo", "bar,baz"]             | 3
        "transfer-encoding"   | ["foo", "bar,baz"]             | 3
        "upgrade"             | ["foo", "bar,baz"]             | 3
        "warning"             | ["foo", "bar,baz"]             | 3
        "accept-encoding"     | ["foo", "bar,baz"]             | 3
        "X-PP-Next-Route"     | ["foo", "bar,baz"]             | 3
        "X-PP-User"           | ["foo", "bar,baz"]             | 3
        "X-PP-Groups"         | ["foo", "bar,baz"]             | 3
        "x-catalog"           | ["foo", "bar,baz"]             | 3
        "X-Authorization"     | ["foo;q=0.1", "bar,baz"]       | 3
        "X-Identity-Status"   | ["foo", "bar,baz"]             | 3
        "X-User-Name"         | ["foo", "bar,baz"]             | 3
        "X-User-Id"           | ["foo", "bar;q=0.2,baz;q=0.3"] | 3
        "X-Tenant-Name"       | ["foo", "bar,baz"]             | 3
        "X-Tenant-Id"         | ["foo", "bar,baz"]             | 3
        "X-Roles"             | ["foo", "bar,baz"]             | 3
        "X-Impersonator-Id"   | ["foo", "bar,baz"]             | 3
        "X-Impersonator-Name" | ["foo", "bar,baz"]             | 3
        "X-Default-Region"    | ["foo", "bar,baz"]             | 3
        "x-token-expires"     | ["foo", "bar,baz"]             | 3
        "X-CONTACT-ID"        | ["foo", "bar,baz"]             | 3
        "X-TTL"               | ["foo", "bar,baz"]             | 3
        "whatever"            | ["foo", "bar,baz"]             | 2
    }

    def getEnumerationLength(Enumeration<String> enumeration) {
        int total = 0
        while (enumeration.hasMoreElements()) {
            total++
            enumeration.nextElement()
        }
        total
    }
}
