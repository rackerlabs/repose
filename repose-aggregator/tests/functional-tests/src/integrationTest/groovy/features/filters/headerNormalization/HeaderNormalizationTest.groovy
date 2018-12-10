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
package features.filters.headerNormalization

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class HeaderNormalizationTest extends ReposeValveTest {

    static final HEADERS = [
        'user1'             : 'usertest1',
        'X-Auth-Token'      : '358484212:99493',
        'X-First-Filter'    : 'firstValue',
        'X-SeCoND-Filter'   : 'secondValue',
        'X-third-filter'    : 'thirdValue',
        'X-last-Filter'     : 'lastValue',
        'X-Shared'          : 'shared',
        'X-User-Token'      : 'something'
    ]

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headerNormalization", params)

        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.targetPort, defaultHandler: { new Response(200, null, HEADERS) })
    }

    def "When Filtering Based on URI and Method"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : reposeEndpoint + "/v1/usertest1/servers/something",
                                headers: HEADERS
                        ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.findAll("x-auth-token") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-shared") == 'shared'
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.findAll("x-auth-token") == []
        mc.receivedResponse.headers.findAll("x-first-filter") == []
        mc.receivedResponse.headers.findAll("x-second-filter") == []
        mc.receivedResponse.headers.findAll("x-third-filter") == []
        mc.receivedResponse.headers.findAll("x-last-filter") == []
        mc.receivedResponse.headers.getFirstValue("x-shared") == 'shared'
    }

    def "When Filtering Based on URI"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'POST',
                                url    : reposeEndpoint + "/v1/usertest1/servers/something",
                                headers: HEADERS
                        ])

        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.findAll("x-auth-token") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-third-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-shared") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.findAll("x-auth-token") == []
        mc.receivedResponse.headers.findAll("x-first-filter") == []
        mc.receivedResponse.headers.findAll("x-second-filter") == []
        mc.receivedResponse.headers.findAll("x-third-filter") == []
        mc.receivedResponse.headers.findAll("x-last-filter") == []
        mc.receivedResponse.headers.findAll("x-shared") == []

    }

    def "When Filtering Based on Method"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'POST',
                                url    : reposeEndpoint + "/v1/usertest1/resources/something",
                                headers: HEADERS
                        ])
        then:
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1
        mc.orphanedHandlings[0].request.headers.findAll("x-auth-token") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-first-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-second-filter") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.orphanedHandlings[0].request.headers.findAll("x-last-filter") == []
        mc.orphanedHandlings[0].request.headers.findAll("x-shared") == []
        mc.orphanedHandlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.findAll("x-auth-token") == []
        mc.receivedResponse.headers.findAll("x-first-filter") == []
        mc.receivedResponse.headers.findAll("x-second-filter") == []
        mc.receivedResponse.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.receivedResponse.headers.findAll("x-last-filter") == []
        mc.receivedResponse.headers.findAll("x-shared") == []
    }

    def "When Filtering using catch all"() {
        when:
        MessageChain mc =
                deproxy.makeRequest(
                        [
                                method : 'GET',
                                url    : reposeEndpoint + "/v1/usertest1/resources/something",
                                headers: HEADERS
                        ])
        then:
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.handlings[0].request.headers.findAll("x-auth-token") == []
        mc.handlings[0].request.headers.getFirstValue("x-user-token") == 'something'
        mc.handlings[0].request.headers.getFirstValue("user1") == 'usertest1'
        mc.handlings[0].request.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.handlings[0].request.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.handlings[0].request.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.handlings[0].request.headers.findAll("x-last-filter") == []
        mc.handlings[0].request.headers.getFirstValue("x-shared") == 'shared'
        mc.handlings[0].request.headers.getFirstValue("via").contains("1.1 localhost:${properties.reposePort} (Repose/")
        mc.receivedResponse.code == '200'
        mc.receivedResponse.headers.findAll("x-auth-token") == []
        mc.receivedResponse.headers.getFirstValue("x-user-token") == 'something'
        mc.receivedResponse.headers.getFirstValue("user1") == 'usertest1'
        mc.receivedResponse.headers.getFirstValue("x-first-filter") == 'firstValue'
        mc.receivedResponse.headers.getFirstValue("x-second-filter") == 'secondValue'
        mc.receivedResponse.headers.getFirstValue("x-third-filter") == 'thirdValue'
        mc.receivedResponse.headers.findAll("x-last-filter") == []
        mc.receivedResponse.headers.getFirstValue("x-shared") == 'shared'
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "$reposeEndpoint/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName     | headerValue
        "x-hdr"        | "123445"
        "X-hdr"        | "239853"
        "x-hdr"        | "slDSFslk&D"
        "x-hdr"        | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
    }
}
