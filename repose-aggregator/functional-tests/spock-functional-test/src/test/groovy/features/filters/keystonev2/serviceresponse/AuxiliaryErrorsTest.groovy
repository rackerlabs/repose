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
package features.filters.keystonev2.serviceresponse

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.openrepose.commons.utils.http.HttpDate
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.springframework.http.HttpHeaders
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE
import static org.openrepose.core.filter.logic.FilterDirector.SC_TOO_MANY_REQUESTS

class AuxiliaryErrorsTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/zerocachetime", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def setup() {
        fakeIdentityService.resetHandlers()

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll("Identity Service Broken Admin Call: #adminBroken Broken Token Validation Call: #validateBroken Broken Groups Call: #groupsBroken Error Code: #errorCode")
    def "When the identity service endpoint returns failed or unexpected responses"() {

        given: "When Calls to Auth Return bad responses"
        if (adminBroken) {
            fakeIdentityService.generateTokenHandler = { request, xml -> return new Response(errorCode) }
        }
        if (validateBroken) {
            fakeIdentityService.validateTokenHandler = { tokenId, request, xml -> return new Response(errorCode) }
        }
        if (groupsBroken) {
            fakeIdentityService.getGroupsHandler = { userId, request, xml -> return new Response(errorCode) }
        }
        def tokenId = "${adminBroken} + ${validateBroken} + ${groupsBroken} + ${errorCode}"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token + tokenId])

        then:
        "User should receive a " + expectedCode + "response"
        mc.receivedResponse.code == expectedCode
        sleep(500)

        where:
        adminBroken | validateBroken | groupsBroken | errorCode | expectedCode
        true        | false          | false        | 400       | "500"
        true        | false          | false        | 401       | "500"
        true        | false          | false        | 402       | "500"
        true        | false          | false        | 403       | "500"
        true        | false          | false        | 404       | "500"
        true        | false          | false        | 413       | "503"
        true        | false          | false        | 429       | "503"
        true        | false          | false        | 500       | "500"
        true        | false          | false        | 501       | "500"
        true        | false          | false        | 502       | "500"
        true        | false          | false        | 503       | "500"

        false       | true           | false        | 400       | "500"
        false       | true           | false        | 401       | "500"
        false       | true           | false        | 402       | "500"
        false       | true           | false        | 403       | "500"
        false       | true           | false        | 404       | "401"
        false       | true           | false        | 413       | "503"
        false       | true           | false        | 429       | "503"
        false       | true           | false        | 500       | "500"
        false       | true           | false        | 501       | "500"
        false       | true           | false        | 502       | "500"
        false       | true           | false        | 503       | "500"

        false       | false          | true         | 400       | "500"
        false       | false          | true         | 401       | "500"
        false       | false          | true         | 402       | "500"
        false       | false          | true         | 403       | "500"
        false       | false          | true         | 404       | "500"
        false       | false          | true         | 413       | "503"
        false       | false          | true         | 429       | "503"
        false       | false          | true         | 500       | "500"
        false       | false          | true         | 501       | "500"
        false       | false          | true         | 502       | "500"
        false       | false          | true         | 503       | "500"
    }


    @Unroll("Sending request with mock identity response set to HTTP #identityStatusCode and Retry-After header")
    def "when failing to authenticate client because of temporary failures"() {
        given: "the HTTP authentication header containing the client token and a Mock Identity return status of HTTP #identityStatusCode"

        def retryTimeStamp = DateTime.now().plusMinutes(5)
        def retryString = new HttpDate(retryTimeStamp.toGregorianCalendar().getTime()).toRFC1123()

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(identityStatusCode, null, [(HttpHeaders.RETRY_AFTER): retryString], xml)
            }
        }
        reposeLogSearch.cleanLog()

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "request body sent from repose to the origin service should contain HTTP 503"
        mc.receivedResponse.code == SC_SERVICE_UNAVAILABLE.toString()
        mc.receivedResponse.getHeaders().getFirstValue(HttpHeaders.RETRY_AFTER).equals(retryString)
        reposeLogSearch.searchByString("Missing ${HttpHeaders.RETRY_AFTER} header on Auth Response status code: $identityStatusCode").size() == 0

        where:
        reqTenant | identityStatusCode
        1111      | SC_REQUEST_ENTITY_TOO_LARGE
        1112      | SC_TOO_MANY_REQUESTS
    }

}
