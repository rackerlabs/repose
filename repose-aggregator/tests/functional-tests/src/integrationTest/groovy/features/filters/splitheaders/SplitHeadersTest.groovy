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
package features.filters.splitheaders

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class SplitHeadersTest extends ReposeValveTest {

    def static originServicePort
    def static reposePort
    def static url
    def static leaveAloneName = "Leave-Alone"
    def static leaveAloneValue = "FooServer;ver=1.0, BarServer 5000"
    def static leaveAlone = [(leaveAloneName): leaveAloneValue]

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = properties.targetPort
        deproxy.addEndpoint(originServicePort)

        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/splitheaders", params)

        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false, waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)
    }

    @Unroll
    def "Requests - headers: #headerName with \"#headerValue\" keep its case"() {
        when: "make a request with the given header and value"
        MessageChain mc = deproxy.makeRequest(url: url, headers: [(headerName): headerValue] + leaveAlone)

        then: "the request should make it to the origin service"
        mc.handlings.size() == 1

        and: "the request should keep headerName case"
        mc.handlings[0].request.headers.contains(headerName)

        and: "the request should keep headerValue case"
        def headerValues = headerValue.tokenize(',').collect { it.trim() }
        mc.handlings[0].request.headers.findAll(headerName).containsAll(headerValues)

        and: "the request should leave the un-configured header alone"
        mc.handlings[0].request.headers.getFirstValue(leaveAloneName) == leaveAloneValue

        where:
        headerName       | headerValue
        "request-test-1" | "split"
        "Request-Test-1" | "One,Two,Three"
        "rEQUEST-tEST-1" | "fOUR,  fIVE,  sIX"
        "REQUEST-TEST-1" | "SEVEN"
        "ReQuEsT-TeSt-2" | "OnE;TwO=2;ThReE=3"
        "rEqUeSt-tEsT-3" | "oNe,tWo,tHrEe"
    }

    @Unroll
    def "Responses - headers: #headerName with \"#headerValue\" keep its case"() {
        when: "make a request with the given header and value in the response"
        MessageChain mc = deproxy.makeRequest(
            url: url,
            defaultHandler: { new Response(200, null, [(headerName): headerValue] + leaveAlone) })

        then: "the request should make it to the origin service"
        mc.handlings.size() == 1

        and: "the request should keep headerName case"
        mc.receivedResponse.headers.contains(headerName)

        and: "the request should keep headerValue case"
        def headerValues = headerValue.tokenize(',').collect { it.trim() }
        mc.receivedResponse.headers.findAll(headerName).containsAll(headerValues)

        and: "the request should leave the un-configured header alone"
        mc.receivedResponse.headers.getFirstValue(leaveAloneName) == leaveAloneValue

        where:
        headerName        | headerValue
        "response-test-1" | "split"
        "Response-Test-1" | "One,Two,Three"
        "rESPONSE-tEST-1" | "fOUR,  fIVE,  sIX"
        "RESPONSE-TEST-1" | "SEVEN"
        "ReSpOnSe-TeSt-2" | "OnE;TwO=2;ThReE=3"
        "rEsPoNsE-tEsT-3" | "oNe,tWo,tHrEe"
    }
}
