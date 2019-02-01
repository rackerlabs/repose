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
package features.filters.cors

import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.CorsHttpHeader
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Filters)
class CorsFilterBasicTest extends ReposeValveTest {

    // The CORS filter will add all of the current response headers to the 'Access-Control-Expose-Headers' list, but this
    // leaves out the list of response headers added after the CORS filter processes the response. Some of these headers
    // will be hard-coded to always be added because of this (e.g. Content-Length), and some of these headers will not
    // be added since they aren't really needed by the client. This is the list of headers that we will not expect to be
    // in the list. If a new response header is added in the future that causes the relevant test to fail, it should
    // either be added to the list of headers we always add in the CorsFilter if the client might need it, or it should
    // be added here if we think the client will not need the contents of the header. The client receives the header
    // either way, but the origin JavaScript would be forbidden from reading the contents of the header in a CORS request.
    static final List<String> UNNECESSARY_HEADERS = [
            CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS,
            CommonHttpHeader.SERVER,
            CommonHttpHeader.TRACE_GUID,
            HttpHeaders.VARY,
            CommonHttpHeader.VIA]*.toLowerCase()

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/cors", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "Non-CORS request with method OPTIONS, 'Access-Control-Request-Method' header of #method, and path #path should make it to origin service"() {
        given: "the request has no 'Origin' header making this a non-CORS request"
        def headers = [(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "none of the CORS headers are added to the response"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase()*.toLowerCase() == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        [method, path] << [['GET', 'HEAD'], ['/', '/status']].combinations()
    }

    def "Actual request with method OPTIONS is not treated like a Preflight request if it does not have an 'Access-Control-Request-Method' header"() {
        given: "the request has an 'Origin' header making this an actual CORS request"
        def origin = 'http://test.repose.site:80'
        def headers = [(CorsHttpHeader.ORIGIN): origin]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + '/testoptions', method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "the CORS headers for an actual request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the CORS headers for a preflight request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase() == ['origin', 'access-control-request-headers', 'access-control-request-method']
    }

    @Unroll
    def "Preflight request with 'Access-Control-Request-Method' header of #method, path #path, and origin #origin should return a 200"() {
        given: "the request has both an 'Origin' and 'Access-Control-Request-Method' header making it a preflight CORS request"
        def headers = [
                (CorsHttpHeader.ORIGIN)                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).tokenize(',').contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is not set since none were requested"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [['GET', 'HEAD'], ['/', '/status'], ['http://openrepose.com:80', 'http://test.repose.site:80']].combinations()
    }

    @Unroll
    def "Preflight request from an unauthorized origin results in a 403 for an 'Access-Control-Request-Method' header of #method, path #path, and origin #origin"() {
        given: "the request has both an 'Origin' and 'Access-Control-Request-Method' header making it a preflight CORS request"
        def headers = [
                (CorsHttpHeader.ORIGIN)                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is Forbidden"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the 'Access-Control-Allow-Origin' header is not set to indicate the 'Origin' was not allowed"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] <<
                [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                 ["/", "/status"],
                 ["http://test.forbidden.com", "http://test.home.site:80", "chrome-extension://fhbjgbiflinjbdggehcddcbncdddomop"]]
                        .combinations()
    }

    @Unroll
    def "Actual request from an unauthorized origin results in a 403 for an 'Access-Control-Request-Method' header of #method, path #path, and origin #origin"() {
        given: "the request has an 'Origin' header making this an actual CORS request"
        def headers = [(CorsHttpHeader.ORIGIN): origin]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then: "the response status is Forbidden"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the 'Access-Control-Allow-Origin' header is not set to indicate the 'Origin' was not allowed"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] <<
                [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                 ["/", "/status"],
                 ["http://test.forbidden.com", "http://test.home.site:80", "chrome-extension://fhbjgbiflinjbdggehcddcbncdddomop"]]
                        .combinations()
    }

    @Unroll
    def "Preflight request for a path matching '/status.*' resource in config will return a 200 with 'Access-Control-Request-Method' header of #method and origin #origin"() {
        given: "the request has both an 'Origin' and 'Access-Control-Request-Method' header making it a preflight CORS request"
        def headers = [
                (CorsHttpHeader.ORIGIN)                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]
        def path = "/status"

        when: "the request is made using a path that will match a specific resource in config"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).tokenize(',').contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is not set since none were requested"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, origin] <<
                [["PUT", "POST", "GET", "HEAD"],
                ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll
    def "Preflight request for a path matching '/testupdate.*' resource in config will return a 200 with 'Access-Control-Request-Method' header of #method and origin #origin"() {
        given: "the request has both an 'Origin' and 'Access-Control-Request-Method' header making it a preflight CORS request"
        def headers = [
                (CorsHttpHeader.ORIGIN)                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]
        def path = "/testupdate"

        when: "the request is made using a path that will match a specific resource in config"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).tokenize(',').contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is not set since none were requested"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, origin] <<
                [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll
    def "Actual request will contain response header 'Access-Control-Expose-Headers' with values #responseHeaders for method #method"() {
        given: "an actual request"
        def path = "/testupdate"
        def origin = 'http://test.repose.site:80'
        def headers = [(CorsHttpHeader.ORIGIN): origin]

        and: "the origin service will return special response headers"
        def handler = { request -> new Response(200, 'OK', (responseHeaders as List<String>).collectEntries{[(it): 'value']})}

        when: "the request is made using the specified method"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers, defaultHandler: handler)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "the CORS headers for an actual request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the CORS headers for a preflight request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "all of the special headers from the origin service are added to the list of values in the 'Access-Control-Expose-Headers' header"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).tokenize(',').containsAll(responseHeaders)

        and: "all of the response headers are in the 'Access-Control-Expose-Headers' header with a few exceptions"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).toLowerCase().tokenize(',').sort() ==
                mc.receivedResponse.headers.names.toList()*.toLowerCase().sort().unique() - UNNECESSARY_HEADERS

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, responseHeaders] << [
                ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                [['x-potatoes'], ['Content-Encoding'], ['x-success', 'Content-Encoding']]].combinations()
    }

    @Unroll
    def "Actual request for a path matching '/testupdate.*' resource in config will return a 200 with origin #origin"() {
        given: "the request has an 'Origin' header making this an actual CORS request"
        def path = "/testupdate"
        def headers = [(CorsHttpHeader.ORIGIN): origin]

        when: "the request is made using a path that will match a specific resource in config"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request makes it to the origin service"
        mc.getHandlings().size() == 1

        and: "the CORS headers for an actual request are added"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the CORS headers for a preflight request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).isEmpty()

        and: "the 'Vary' header is set"
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, origin] <<
                [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll
    def "Preflight request with a path #path that does not match the resource needed for requested method #method to be allowed results in a 403"() {
        given: "a preflight request with an allowed origin"
        def origin = "http://test.repose.site:80"
        def headers = [
                (CorsHttpHeader.ORIGIN)                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD): method]

        when: "the request is made using a path that does not match the resource that would have allowed the requested method"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is Forbidden"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the 'Access-Control-Allow-Origin' header is set correctly since the origin was valid"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).size() == 1

        and: "the 'Access-Control-Allow-Methods' header does not exist"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase() == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        method   | path
        "PATCH"  | "/status"
        "DELETE" | "/status"
        "PATCH"  | "/"
        "DELETE" | "/"
        "PUT"    | "/"
        "POST"   | "/"
    }

    @Unroll
    def "Actual request with a path #path that does not match the resource needed for method #method to be allowed results in a 403"() {
        given: "an actual request with an allowed origin"
        def origin = "http://test.repose.site:80"
        def headers = [(CorsHttpHeader.ORIGIN): origin]

        when: "the request is made using a path that does not match the resource that would have allowed the requested method"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then: "the response status is Forbidden"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the 'Access-Control-Allow-Origin' header is set correctly since the origin was valid"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).size() == 1

        and: "the 'Access-Control-Allow-Methods' header does not exist"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).isEmpty()

        and: "the 'Vary' header is set with the correct values for a non-OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase() == ['origin']

        where:
        method   | path
        "PATCH"  | "/status"
        "DELETE" | "/status"
        "PATCH"  | "/"
        "DELETE" | "/"
        "PUT"    | "/"
        "POST"   | "/"
        "TRACE"  | "/"
    }

    @Unroll
    def "Preflight request with 'Access-Control-Request-Headers' header #requestHeaders results in 'Access-Control-Allow-Headers' header in response with the same values for requested method #method"() {
        given: "an actual request with an allowed origin, valid path, and an 'Access-Control-Request-Headers' header"
        def origin = 'http://openrepose.com:80'
        def path = '/testupdate'
        def headers = [
                (CorsHttpHeader.ORIGIN)                        : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD) : method,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS): requestHeaders
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).tokenize(',').contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is set to the values that were in the 'Access-Control-Request-Headers' header"
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS) == requestHeaders

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase() == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        [method, requestHeaders] <<
                [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                ['x-auth-token', 'x-ponies', 'cookie']].combinations()
    }

    @Unroll
    def "Preflight request with 'Access-Control-Request-Headers' headers #requestHeaders results in response with 'Access-Control-Allow-Headers' headers #allowHeaders"() {
        given: "an actual request with an allowed origin, valid path, and an 'Access-Control-Request-Headers' header"
        def origin = 'http://openrepose.com:80'
        def path = '/testupdate'
        def method = "GET"
        def headers = [
                new Header(CorsHttpHeader.ORIGIN, origin),
                new Header(CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD, method)
        ] + requestHeaders.collect { new Header(CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, it) }

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then: "the response status is OK"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the request does not make it to the origin service"
        mc.getHandlings().isEmpty()

        and: "the CORS headers for a preflight request are added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS).contains(method)
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS) == 'true'

        and: "the 'Access-Control-Allow-Headers' header is set to the values that were in the 'Access-Control-Request-Headers' header without being split"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).tokenize(',').containsAll(allowHeaders)

        and: "the CORS headers for an actual request are not added"
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS).isEmpty()

        and: "the 'Vary' header is set with the correct values for an OPTIONS request"
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll("Vary")*.toLowerCase() == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        requestHeaders                        | allowHeaders
        ["x-auth-token"]                      | ["x-auth-token"]
        ["x-ponies, x-unicorns"]              | ["x-ponies", "x-unicorns"]
        ["x-cupcakes", "x-apple-pie"]         | ["x-cupcakes", "x-apple-pie"]
        ["x-pineapple, x-cranberry, x-grape"] | ["x-pineapple", "x-cranberry", "x-grape"]
        ["x-red", "x-green", "x-blue"]        | ["x-red", "x-green", "x-blue"]
        ["circle, square", "triangle"]        | ["circle", "square", "triangle"]
    }
}
