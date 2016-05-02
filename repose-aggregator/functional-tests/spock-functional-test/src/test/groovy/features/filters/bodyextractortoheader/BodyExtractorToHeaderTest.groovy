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
package features.filters.bodyextractortoheader

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static org.junit.Assert.*

/**
 * Created by jennyvo on 4/28/16.
 *  Verify body extractor to headers
 */
class BodyExtractorToHeaderTest extends ReposeValveTest {
    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/bodyextractortoheader", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def static params
    def static matchDevideBody = """{"bodydata": {"name":"test", "device": "12345", "something": "foo"}}"""
    def static matchServerBody = """{"bodydata": {"name":"test", "server": "abc123", "something": "foo"}}"""
    def static noNatchBody = """{"bodydata": {"name":"test", "something": "foo"}}"""

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll
    def "When request with match config jsonpath will add header as config and its value"() {
        Map headers = ["content-type": "application/json"]
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: reqbody)

        then:
        mc.handlings.size() == 1
        // x-test-param will not added to req since not match request body jsonpath and default not set
        assertFalse(mc.handlings[0].request.headers.contains("x-test-param"))
        if (matchedheaders == "") {
            // doesn't match still add header if default is specified
            assertTrue(mc.handlings[0].request.headers.contains("x-device-id"))
            assertTrue(mc.handlings[0].request.headers.contains("x-server-id"))
            assertEquals(mc.handlings[0].request.headers.getFirstValue("x-device-id"), "test")
            assertEquals(mc.handlings[0].request.headers.getFirstValue("x-server-id"), "test")

        } else {
            assertTrue(mc.handlings[0].request.headers.contains(matchedheaders))
            assertEquals(mc.handlings[0].request.headers.getFirstValue(matchedheaders), headervalue)
            assertEquals(mc.handlings[0].request.headers.getFirstValue(unmatchedheaders), "test")
        }

        where:
        reqbody         | matchedheaders | headervalue | unmatchedheaders
        matchDevideBody | "x-device-id"  | "12345"     | "x-server-id"
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"
        noNatchBody     | ""             | ""          | ""
    }

    def "Override exist header if override=true"() {
        given: "request with previous set of headers"
        Map headers = ["content-type": "application/json", "x-device-id": "54321", "x-server-id": "reposetest123"]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: matchDevideBody)

        then:
        mc.handlings.size() == 1
        assertTrue(mc.handlings[0].request.headers.contains("x-device-id"))
        assertTrue(mc.handlings[0].request.headers.contains("x-server-id"))
        assertEquals(mc.handlings[0].request.headers.getFirstValue("x-device-id"), "12345")
        assertEquals(mc.handlings[0].request.headers.getFirstValue("x-server-id"), "reposetest123")
    }

    def "Not override exist header if override=false"() {
        given: "request with previous set of headers"
        Map headers = ["content-type": "application/json", "x-device-id": "54321", "x-server-id": "reposetest123"]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: matchServerBody)

        then:
        mc.handlings.size() == 1
        assertTrue(mc.handlings[0].request.headers.contains("x-device-id"))
        assertTrue(mc.handlings[0].request.headers.contains("x-server-id"))
        assertEquals(mc.handlings[0].request.headers.getFirstValue("x-device-id"), "test")
        assertTrue(mc.handlings[0].request.headers.findAll("x-server-id").contains("reposetest123"))
        // not override but add header extracted from body
        assertTrue(mc.handlings[0].request.headers.findAll("x-server-id").contains("abc123"))
    }
}
