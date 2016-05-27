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

import framework.ReposeValveTest
import org.openrepose.commons.utils.http.CorsHttpHeader
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/29/15.
 */
class CorsFilterBasicTest extends ReposeValveTest {
    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/cors", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Non-CORS request with method OPTIONS and a CORS header request Method: #method")
    def "OPTIONS request without origin header will bypass CORS and make to origin service"() {
        def headers = [
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 1  // OPTIONS request without origin will make to it origin service
        !mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString())
        !mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString())
        !mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString())
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll('Vary') == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        [method, path, origin] << [['GET', 'HEAD'], ['/', '/status'], ['http://openrepose.com:80', 'http://test.repose.site:80']].combinations()
    }

    def "Actual request with method OPTIONS"() {
        given:
        def origin = 'http://openrepose.com:80'
        def headers = [(CorsHttpHeader.ORIGIN.toString()): origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + '/testoptions', method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 1   // actual request should make it through to the origin service
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        !mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString())
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll('Vary') == ['origin', 'access-control-request-headers', 'access-control-request-method']
    }

    @Unroll("Preflight request with origin #origin and request method #method ")
    def "When sending preflight request with cors filter, the specific headers should be added for requested method #method, origin #origin, and path #path"() {
        given:
        def headers = [
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 0  // preflight request doesn't make it to the origin service
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [['GET', 'HEAD'], ['/', '/status'], ['http://openrepose.com:80', 'http://test.repose.site:80']].combinations()
    }

    @Unroll("Prefight request Not allowed Origin request header with method #method, path #path, and origin #origin")
    def "Origin request header is not allow in the config response 403"() {
        given:
        def headers = [
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '403'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                                   ["/", "/status"],
                                   ["http://test.forbidden.com", "http://test.home.site:80"]].combinations()
    }

    @Unroll("Actual request Not allowed Origin request header with method #method, path #path, and origin #origin")
    def "Actual Request with Origin request header is not allow in the config response 403"() {
        given:
        def headers = [(CorsHttpHeader.ORIGIN.toString()): origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        mc.receivedResponse.code == '403'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                                   ["/", "/status"],
                                   ["http://test.forbidden.com", "http://test.home.site:80"]].combinations()
    }

    @Unroll("Preflight request, Allowed resource and Origin request header with method #method, path #path, and origin #origin")
    def "Origin request header is allow in the config"() {
        given:
        def headers = [
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [["PUT", "POST", "GET", "HEAD"], ["/status"],
                                   ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll("Preflight request, Allowed Resource with Origin request header with method #method, path #path, and origin #origin")
    def "Origin request header is allow resource in the config"() {
        given:
        def headers = [
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"], ["/testupdate"],
                                   ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll
    def "Actual request with allowed Resource with Origin request header with method #method, path #path, and origin #origin and headers #responseHeaders"() {
        given:
        def headers = [(CorsHttpHeader.ORIGIN.toString()): origin]
        // have deproxy add the response headers
        def handler = { request -> new Response(200, 'OK', (responseHeaders as List<String>).collectEntries{[(it): 'value']})}

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers, defaultHandler: handler)

        then:

        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 1
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        !mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString())
        !mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString())
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains('Vary')

        where:
        [method, path, origin, responseHeaders] << [
                ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                ["/testupdate"],
                ['http://openrepose.com:80', "http://test.repose.site:80"],
                [['x-potatoes'], ['Content-Encoding'], ['x-success', 'Content-Encoding']]].combinations()
    }

    @Unroll("Actual request, Allowed Resource with Origin request header with method #method, path #path, and origin #origin")
    def "Actual request with allow resource in the config"() {
        given:
        def headers = [(CorsHttpHeader.ORIGIN.toString()): origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 1
        mc.receivedResponse.headers.contains("Vary")

        where:
        [method, path, origin] << [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"], ["/testupdate"],
                                   ['http://openrepose.com:80', "http://test.repose.site:80"]].combinations()
    }

    @Unroll("Preflight request, Not allowed resource with origin request header with method #method, path #path, and origin #origin")
    def "Origin request method is not allow resource in the config"() {
        given:
        def headers = [
                (CorsHttpHeader.ORIGIN.toString())                       : origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '403'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).size() == 1
        !mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.contains("Vary")
        mc.receivedResponse.headers.findAll('Vary') == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        method   | path      | origin
        "PATCH"  | "/status" | 'http://openrepose.com:80'
        "DELETE" | "/status" | "http://test.repose.site:80"
        "PATCH"  | "/"       | 'http://openrepose.com:80'
        "DELETE" | "/"       | "http://test.repose.site:80"
        "PUT"    | "/"       | 'http://openrepose.com:80'
        "POST"   | "/"       | "http://test.repose.site:80"

    }

    @Unroll("Actual Request Not allowed resource with method #method, path #path, and origin #origin")
    def "Actual Request request method is not allow resource in the config"() {
        given:
        def headers = [(CorsHttpHeader.ORIGIN.toString()): origin]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        mc.receivedResponse.code == '403'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).size() == 1
        !mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.contains('Vary')
        mc.receivedResponse.headers.findAll('Vary') == ['origin']

        where:
        method    | path      | origin
        "PATCH"   | "/status" | 'http://openrepose.com:80'
        "DELETE"  | "/status" | 'http://test.repose.site:80'
        "PATCH"   | "/"       | 'http://openrepose.com:80'
        "DELETE"  | "/"       | 'http://test.repose.site:80'
        "PUT"     | "/"       | 'http://openrepose.com:80'
        "POST"    | "/"       | 'http://test.repose.site:80'
        "TRACE"   | "/"       | 'http://openrepose.com:80'
    }

    @Unroll
    def "Preflight request has preflight CORS headers for request method #method and headers #requestHeaders"() {
        given:
        def origin = 'http://openrepose.com:80'
        def path = '/testupdate'
        def headers = [
                (CorsHttpHeader.ORIGIN.toString()): origin,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_METHOD.toString()): method,
                (CorsHttpHeader.ACCESS_CONTROL_REQUEST_HEADERS.toString()): requestHeaders
        ]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'OPTIONS', headers: headers)

        then:
        mc.receivedResponse.code == '200'
        mc.getHandlings().size() == 0
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()) == origin
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.toString()).size() == 1
        mc.receivedResponse.headers.findAll(CorsHttpHeader.ACCESS_CONTROL_ALLOW_METHODS.toString()).contains(method)
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_HEADERS.toString()) == requestHeaders
        !mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS.toString())
        mc.receivedResponse.headers.getFirstValue(CorsHttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString()) == 'true'
        mc.receivedResponse.headers.contains('Vary')
        mc.receivedResponse.headers.findAll('Vary') == ['origin', 'access-control-request-headers', 'access-control-request-method']

        where:
        [method, requestHeaders] << [["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"],
                                     ['x-auth-token', 'x-auth-token, x-ponies', 'cookie']].combinations()
    }
}
