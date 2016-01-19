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

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.codehaus.jettison.json.JSONObject
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/19/14.
 * This test checking TRACE log configuration.
 */
class IntrafilterLoggingTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        //remove old log
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/intrafilterlogging", params)

        repose.start([waitOnJmxAfterStarting: false])

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    @Unroll("when sending token #sendtoken respcode will be #respcode")
    def "Check TRACE log entries for when req sent to Repose with client-auth and ip-identity filter"() {
        given:
        fakeIdentityService.client_token = sendtoken

        def responseBody = respbody
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/server123", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityService.client_token],
                defaultHandler: { request ->
                    return new Response(respcode, respmsg, ["Content-Type": type], responseBody)
                })

        //def sentRequest = mc.getHandlings()[0]

        then: "checking for response code and handlings"
        mc.receivedResponse.code == respcode
        mc.handlings.size() == 1
        reposeLogSearch.searchByString("TRACE intrafilter-logging").size() == 4
        reposeLogSearch.searchByString("Intrafilter Request Log").size() == 2
        reposeLogSearch.searchByString("Intrafilter Response Log").size() == 2

        // This part of test what will be in the TRACE log to expect
        and: "checking for client-auth - request"

        JSONObject authreqline1 = convertToJson("Intrafilter Request Log", 0)
        assertHeadersExists(["X-Auth-Token", "Intrafilter-UUID"], authreqline1)
        assertKeyValueMatch([
                "currentFilter": "client-auth",
                "httpMethod"   : "GET",
                "requestURI"   : "/servers/server123",
                "requestBody"  : ""
        ], authreqline1)
        def requestId = authreqline1.get("headers").get("Intrafilter-UUID")

        and: "checking for client-auth - response"
        //THIS IS 2ND IN THE LIST BECAUSE IT'S A STACK
        def authrespline1 = convertToJson("Intrafilter Response Log", 1)
        // Due to the way this is tested it is Case-Sensitive.
        assertHeadersExists(["Intrafilter-UUID", "content-type"], authrespline1)
        assertKeyValueMatch([
                "currentFilter"   : "client-auth",
                "responseBody"    : responseBody,
                "httpResponseCode": respcode
        ], authrespline1)
        requestId == authrespline1.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - request"
        def authreqline2 = convertToJson("Intrafilter Request Log", 1)
        assertHeadersExists(["X-Auth-Token", "x-pp-user", "x-pp-groups", "x-tenant-name",
                             "x-user-id", "x-authorization", "Intrafilter-UUID"], authreqline2)
        assertKeyValueMatch([
                "currentFilter": "context_1-ip-identity",
                "httpMethod"   : "GET",
                "requestURI"   : "/servers/server123",
                "requestBody"  : ""
        ], authreqline2)
        requestId == authreqline2.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - response"
        //THIS IS FIRST ON THE LIST BECAUSE IT'S A STACK
        def authrespline2 = convertToJson("Intrafilter Response Log", 0)
        // Due to the way this is tested it is Case-Sensitive.
        assertHeadersExists(["Intrafilter-UUID", "content-type"], authrespline2)
        assertKeyValueMatch([
                "currentFilter"   : "context_1-ip-identity",
                "responseBody"    : responseBody,
                "httpResponseCode": respcode
        ], authrespline2)
        requestId == authrespline2.get("headers").get("Intrafilter-UUID")

        where:
        sendtoken                  | respcode | respmsg         | type               | respbody
        "this-is-a-token"          | "200"    | "OK"            | "application/json" | "{\"response\": \"amazing\""
        "this-is-an-invalid-token" | "401"    | "Invalid Token" | "plain/text"       | "{\"response\": \"Unauthorized\""
    }

    def "ensure that intrafilter logging isn't munching the x-pp-user headers"() {
        given:
        fakeIdentityService.client_token = "this-is-a-token"

        when: "User passes a request through repose with multiple x-pp-groups headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/server123", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityService.client_token,
                          'x-pp-user'   : "Developers;q=1.0 , Secure Developers;q=0.9 , service:admin-role1 , member"],
                defaultHandler: { request ->
                    return new Response(200, "OK", [
                            "Content-Type": "application/json",
                            "x-pp-user"   : "one,two,three"
                    ], """{"response": "amazing"}""")
                })

        then: "Making sure it was logged, and that it went through repose correctly"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        reposeLogSearch.searchByString("TRACE intrafilter-logging").size() == 4
        reposeLogSearch.searchByString("Intrafilter Request Log").size() == 2
        reposeLogSearch.searchByString("Intrafilter Response Log").size() == 2

        // This part of test what will be in the TRACE log to expect
        and: "Checking to make sure that we didn't clobber the x-pp-user header entries in the log output"

        JSONObject authreqline1 = convertToJson("Intrafilter Request Log", 0)

        //Need to assert that the splittable headers are logged properly, not just the first one (or the last one)
        //get the headers object out
        def xPPUsers = authreqline1.get("headers").get("x-pp-user").split(",")

        //We expect that THERE ARE FOUR LIGHTS.... er entries in the xPPUser header
        xPPUsers.length == 4

        and: "Need to verify the response while we're at it"
        JSONObject responseLine1 = convertToJson("Intrafilter Response Log", 1)
        def responseUsers = responseLine1.get("headers").get("x-pp-user").split(",")
        responseUsers.length == 3
    }

    private JSONObject convertToJson(String searchString, int entryNumber) {
        def reqString = reposeLogSearch.searchByString(searchString).get(entryNumber)
        def requestJson = reqString.substring(reqString.indexOf("{\"preamble\""))
        println requestJson

        return new JSONObject(requestJson)
    }

    private assertHeadersExists(List headers, JSONObject jsonObject) {
        // Due to the way this is tested it is Case-Sensitive.
        headers.each {
            headerName ->
                assert jsonObject.get("headers").has(headerName)
        }
    }

    private assertKeyValueMatch(Map headers, JSONObject jsonObject) {
        headers.each {
            key, value ->
                assert jsonObject.get(key) == value

        }

    }
}
