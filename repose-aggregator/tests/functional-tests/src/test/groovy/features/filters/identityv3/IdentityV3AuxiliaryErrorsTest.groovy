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
package features.filters.identityv3

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
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

/**
 * Created by jamesc on 1/18/15.
 */
class IdentityV3AuxiliaryErrorsTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        if (repose)
            repose.stop()
    }

    def setup() {
        sleep 500
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll("Sending request with admin response set to HTTP #adminRespCode receive #respCode")
    def "when failing to authenticate admin client"() {
        given:
        fakeIdentityV3Service.with {
            client_domainid = reqDomain
            client_userid = reqDomain
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request ->
                    new Response(adminRespCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == respCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        where:
        reqDomain | adminRespCode | respCode | responseBody                                              | orphanedHandlings
        1111      | 500           | "500"    | ""                                                        | 1
        1112      | 404           | "500"    | ""                                                        | 1
        1113      | 401           | "500"    | fakeIdentityV3Service.identityFailureAuthJsonRespTemplate | 1
        1113      | 200           | "500"    | ""                                                        | 1
    }


    @Unroll("Sending authentication request with mock identity response set to HTTP #identityStatusCode and Retry-After header")
    def "when failing to authenticate because of temporary failures"() {
        given: "the HTTP authentication header containing the client token and a Mock Identity return status of HTTP #identityStatusCode"

        def retryTimeStamp = DateTime.now().plusMinutes(5)
        def retryString = new HttpDate(retryTimeStamp.toGregorianCalendar().getTime()).toRFC1123()


        fakeIdentityV3Service.with {
            client_domainid = reqDomain
            client_userid = reqDomain
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request -> new Response(identityStatusCode, null, [(HttpHeaders.RETRY_AFTER): retryString], "")
            }

        }

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "request body sent from repose to the origin service should contain HTTP 503"
        mc.receivedResponse.code == SC_SERVICE_UNAVAILABLE.toString()
        mc.receivedResponse.getHeaders().getFirstValue(HttpHeaders.RETRY_AFTER).equals(retryString)
        reposeLogSearch.searchByString("Missing ${HttpHeaders.RETRY_AFTER} header on Auth Response status code: $identityStatusCode").size() == 0

        where:
        reqDomain | identityStatusCode
        1115      | SC_REQUEST_ENTITY_TOO_LARGE
        1116      | SC_TOO_MANY_REQUESTS

    }

}
