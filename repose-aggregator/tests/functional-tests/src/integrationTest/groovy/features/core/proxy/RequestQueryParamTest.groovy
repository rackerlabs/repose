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
package features.core.proxy

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class RequestQueryParamTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()

    }

    @Unroll("When client requests: #method #uriSuffixGiven, repose should normalize to: #uriSuffixExpected")
    def "when given a query param list, Repose should forward a valid query param list"() {

        when: "the client makes a request through Repose"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + uriSuffixGiven, method: method)


        then: "after passing through Repose, request path should contain a valid query param list"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path.endsWith(uriSuffixExpected)


        where: "given a path with query params defined"
        uriSuffixGiven                       | uriSuffixExpected                   | method
        "/path/to/resource?"                 | "/path/to/resource"                 | "GET"
        "/path/to/resource?"                 | "/path/to/resource"                 | "POST"
        "/path/to/resource?="                | "/path/to/resource"                 | "GET"
        "/path/to/resource?="                | "/path/to/resource"                 | "POST"
        "/path/to/resource?&"                | "/path/to/resource"                 | "GET"
        "/path/to/resource?&"                | "/path/to/resource"                 | "POST"
        "/path/to/resource?=&"               | "/path/to/resource"                 | "GET"
        "/path/to/resource?=&"               | "/path/to/resource"                 | "POST"
        "/path/to/resource?=&="              | "/path/to/resource"                 | "GET"
        "/path/to/resource?=&="              | "/path/to/resource"                 | "POST"
        "/path/to/resource?&=&"              | "/path/to/resource"                 | "GET"
        "/path/to/resource?&=&"              | "/path/to/resource"                 | "POST"
        "/path/to/resource?a=12345"          | "/path/to/resource?a=12345"         | "GET"
        "/path/to/resource?a=12345"          | "/path/to/resource?a=12345"         | "POST"
        "/path/to/resource?&a=12345"         | "/path/to/resource?a=12345"         | "GET"
        "/path/to/resource?&a=12345"         | "/path/to/resource?a=12345"         | "POST"
        "/path/to/resource/?&a=12345"        | "/path/to/resource/?a=12345"        | "GET"
        "/path/to/resource/?&a=12345"        | "/path/to/resource/?a=12345"        | "POST"
        "/path/to/resource?&a=12345&b=54321" | "/path/to/resource?a=12345&b=54321" | "GET"
        "/path/to/resource?&a=12345&b=54321" | "/path/to/resource?a=12345&b=54321" | "POST"
    }

    @Unroll("when given an improperly encoded URI character, Repose should properly encode it - #uriSuffixGiven")
    def "when given an improperly encoded URI character, Repose should properly encode it"() {

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uriSuffixGiven)

        then:
        messageChain.handlings.size() == 1
        messageChain.sentRequest.path.endsWith(uriSuffixGiven)
        messageChain.handlings[0].request.path.endsWith(uriSuffixExpected)

        where:
        uriSuffixGiven                             | uriSuffixExpected
        "/path/to/resource?key=value%2Bvalue"      | "/path/to/resource?key=value%2Bvalue"
        "/path/to/resource?key=value/othervalue"   | "/path/to/resource?key=value%2Fothervalue"
        "/path/to/resource?key=value:value"        | "/path/to/resource?key=value%3Avalue"
        "/path/to/resource?key=value@value"        | "/path/to/resource?key=value%40value"
        "/path/to/resource?key=value?value"        | "/path/to/resource?key=value%3Fvalue"
        "/path/to/resource?key=value[value"        | "/path/to/resource?key=value%5Bvalue"
        "/path/to/resource?key=value]value"        | "/path/to/resource?key=value%5Dvalue"
        "/path/to/resource?key=value%2Fothervalue" | "/path/to/resource?key=value%2Fothervalue"
        "/path/to/resource?key=value%3Avalue"      | "/path/to/resource?key=value%3Avalue"
        "/path/to/resource?key=value%40value"      | "/path/to/resource?key=value%40value"
        "/path/to/resource?key=value%3Fvalue"      | "/path/to/resource?key=value%3Fvalue"
        "/path/to/resource?key=value%5Bvalue"      | "/path/to/resource?key=value%5Bvalue"
        "/path/to/resource?key=value%5Dvalue"      | "/path/to/resource?key=value%5Dvalue"
        "/path/to/resource?key=value%20value"      | "/path/to/resource?key=value+value"
        "/path/to/resource?key=value+value"        | "/path/to/resource?key=value+value"
    }

    @Unroll("when given a URI with a malformed query parameter, Repose should reject the request with a 400 - #uriSuffixGiven")
    def "when given a URI with a malformed query parameter, Repose should reject the request with a 400"() {

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uriSuffixGiven)

        then:
        messageChain.handlings.size() == 0
        messageChain.sentRequest.path.endsWith(uriSuffixGiven)
        messageChain.receivedResponse.code == "400"

        where:
        uriSuffixGiven << [
            "/path/to/resource?key=value value",
            "/path/to/resource?key=value@%",
            "/path/to/resource?key=va%lu@e"
        ]
    }

    def "when given a query param name that is encoded, repose shouldn't encode it again"() {
        given: "a path with an encoded query parameter name"
        def pathWithQuery = "/path/to/resource?foo%5B%5D=bar"

        when: "the client makes a request through Repose"
        MessageChain messageChain = deproxy.makeRequest(url: "$reposeEndpoint$pathWithQuery", method: "GET")


        then: "after passing through Repose, request path should contain the same parameter list"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path.endsWith(pathWithQuery)
    }
}
