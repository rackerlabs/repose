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
package features.core.powerfilter

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response


class HeaderParserTest extends ReposeValveTest {

    def static String locations = "/v1/queues/mqueue/messages?ids=locationOne,locationTwo"

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def "when expecting a comma-separated location header"() {
        given: "Origin service returns a comma-separated location header"
        def headerResp = { request ->
            return new Response(200, "OK",
                    ["Location": locations], "")
        }

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint,
                method: "GET",
                headers: ["x-test": "test"],
                requestBody: "",
                defaultHandler: headerResp)

        then: "Repose returns a comma-separated location header"
        resp.getReceivedResponse().getHeaders().getFirstValue("Location").equals(locations)
    }

    def "when client sends a Location header with an un-escaped comma, then Repose should pass it through unchanged"() {

        def locations2 = "/path/to/resource?ids=valueOne,valueTwo"

        when: "Client sends Location header in a request with an un-escaped comma"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: ['Location': locations2])

        then: "Repose should pass the header to the origin service unchanged"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("Location") == 1
        mc.handlings[0].request.headers["Location"] == locations2

    }

    def "when client sends a Location header with an escaped comma, then Repose should pass it through unchanged"() {

        def locations2 = "/path/to/resource?ids=valueOne%2CvalueTwo"

        when: "Client sends Location header in a request with an un-escaped comma"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: ['Location': locations2])

        then: "Repose should pass the header to the origin service unchanged"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("Location") == 1
        mc.handlings[0].request.headers["Location"] == locations2

    }
}
