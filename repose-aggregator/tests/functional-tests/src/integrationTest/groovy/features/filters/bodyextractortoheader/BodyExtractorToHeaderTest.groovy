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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Created by jennyvo on 4/28/16.
 *  Verify body extractor to headers
 */
@Category(Filters)
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
    def static matchDeviceBody = """{"bodyData": {"name":"test", "device": "12345", "something": "foo"}}"""
    def static matchServerBody = """{"bodyData": {"name":"test", "server": "abc123", "something": "foo"}}"""
    def static noMatchBody = """{"bodyData": {"name":"test", "something": "foo"}}"""
    def static notNullNatchBody = """{"bodyData": {"name":"test", "xyz": "rst987", "something": "foo"}}"""
    def static nullMatchBody = """{"bodyData": {"name":"test", "xyz": null, "something": "foo"}}"""
    def static malformedJson = """{"bodyData": {"name":"test", "server": "abc123", "something": "foo"}"""
    def static malformedJson2 = """{"bodyData"{"name":"test", "device": "12345", "something": "foo"}}"""

    @Unroll
    def "When request with match config jsonpath will add header as config and its value"() {
        Map headers = ["content-type": contentheader]
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: reqbody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        // x-test-param will not added to req since not match request body jsonpath and default not set
        !mc.handlings[0].request.headers.contains("x-test-param")
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
        reqbody         | matchedheaders | headervalue | unmatchedheaders | contentheader
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | "application/json"
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | "application/json"
        noMatchBody     | ""             | ""          | ""               | "application/json"
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | "application/atpm+json"
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | "application/atpm+json"
        noMatchBody     | ""             | ""          | ""               | "application/atpm+json"
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | "json"
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | "json"
        noMatchBody     | ""             | ""          | ""               | "json"
    }

    def "Override exist header if override=true"() {
        given: "request with previous set of headers"
        Map headers = ["content-type": "application/json", "x-device-id": "54321", "x-server-id": "reposeTest123"]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: matchDeviceBody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("x-device-id")
        mc.handlings[0].request.headers.contains("x-server-id")
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == "12345"
        mc.handlings[0].request.headers.getFirstValue("x-server-id") == "reposeTest123"
    }

    def "Not override exist header if override=false"() {
        given: "request with previous set of headers"
        Map headers = ["content-type": "application/json", "x-device-id": "54321", "x-server-id": "reposeTest123"]

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: matchServerBody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("x-device-id")
        mc.handlings[0].request.headers.contains("x-server-id")
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == "test"
        mc.handlings[0].request.headers.findAll("x-server-id").contains("reposeTest123")
        // not override but add header extracted from body
        mc.handlings[0].request.headers.findAll("x-server-id").contains("abc123")
    }

    def "Replace JSON null value and add the defaults"() {
        given: "request with proper Content-Type header and body with a JSON Null value"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["content-type": "application/json"], requestBody: nullMatchBody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("x-device-id")
        mc.handlings[0].request.headers.contains("x-server-id")
        mc.handlings[0].request.headers.contains("x-null-param")
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == "test"
        mc.handlings[0].request.headers.getFirstValue("x-server-id") == "test"
        mc.handlings[0].request.headers.getFirstValue("x-null-param") == "987zyx"
    }

    def "Do NOT replace JSON value that is NOT null and add the defaults"() {
        given: "request with proper Content-Type header and body with a JSON Null value"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: ["content-type": "application/json"], requestBody: notNullNatchBody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.contains("x-device-id")
        mc.handlings[0].request.headers.contains("x-server-id")
        mc.handlings[0].request.headers.contains("x-null-param")
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == "test"
        mc.handlings[0].request.headers.getFirstValue("x-server-id") == "test"
        mc.handlings[0].request.headers.getFirstValue("x-null-param") == "rst987"
    }

    @Unroll
    def "Missing content-type or wrong content-type"() {
        when: "send request without content-type header or wrong content-type"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: reqheaders, requestBody: reqbody)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings[0].request.headers.findAll("x-server-id").size() == 0
        mc.handlings[0].request.headers.findAll("x-device-id").size() == 0
        mc.handlings[0].request.headers.findAll("x-test-param").size() == 0

        where:
        reqbody         | matchedheaders | headervalue | unmatchedheaders | reqheaders
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | ["content-type": "application/xml"]
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | ["content-type": "application/xml"]
        noMatchBody     | ""             | ""          | ""               | ["content-type": "application/xml"]
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | ["content-type": "text/plain"]
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | ["content-type": "text/plain"]
        noMatchBody     | ""             | ""          | ""               | ["content-type": "text/plain"]
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | ["content-type": ""]
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | ["content-type": ""]
        noMatchBody     | ""             | ""          | ""               | ["content-type": ""]
        matchDeviceBody | "x-device-id"  | "12345"     | "x-server-id"    | null
        matchServerBody | "x-server-id"  | "abc123"    | "x-device-id"    | null
        noMatchBody     | ""             | ""          | ""               | null
    }

    @Unroll
    def "When request with Malformed json body filter will not add config header to request"() {
        Map headers = ["content-type": "application/json"]
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", header: headers, requestBody: malformedJson)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        // do nothing
        !mc.handlings[0].request.headers.contains("x-device-id")
        !mc.handlings[0].request.headers.contains("x-server-id")
        !mc.handlings[0].request.headers.contains("x-null-param")
        !mc.handlings[0].request.headers.contains("x-test-param")

        where:
        reqbody << [malformedJson, malformedJson2]
    }
}
