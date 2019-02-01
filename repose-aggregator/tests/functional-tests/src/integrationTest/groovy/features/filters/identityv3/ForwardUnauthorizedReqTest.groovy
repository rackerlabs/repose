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

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/26/14.
 * Test forward-unauthorized-requests option
 * Acceptance Criteria
 * - When a request is unverified set X-Identity-Status: Indeterminate
 */
@Category(Filters)
class ForwardUnauthorizedReqTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/forwardunauthorizedrequests", params)
        repose.start()
        waitUntilReadyToServiceRequests('200')
    }

    def setup() {
        sleep(500)
        fakeIdentityV3Service.resetHandlers()
    }

    def "when send req without credential with forward-unauthorized-request true"() {

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456/",
                method: 'GET',
                headers: ['content-type': 'application/json'])

        then: "Request body sent from repose to the origin service should contain"
        reposeLogSearch.searchByString("DEBUG org.openrepose.filters.openstackidentityv3.OpenStackIdentityV3Handler - No X-Subject-Token present -- a subject token was not provided to validate").size() > 0
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization")
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
    }

    @Unroll("#authResponseCode, #responseCode")
    def "when send req with unauthorized user with forward-unauthorized-request true"() {
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_projectid = reqProject
            service_admin_role = "not-admin"
        }

        if (authResponseCode != 200) {
            fakeIdentityV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject/",
                method: 'GET',
                headers: ['content-type'   : 'application/json',
                          'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization") == "Proxy"
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"

        where:
        reqProject | authResponseCode | responseCode | responseBody
        "p500"     | 401              | "200"        | "Unauthorized"
        "p501"     | 403              | "200"        | "Unauthorized"
        "p502"     | 404              | "200"        | fakeIdentityV3Service.identityFailureJsonRespTemplate
    }

    def "when client failed to authenticate at the origin service, the WWW-Authenticate header should be expected"() {
        given:
        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                new Response(404, null, null, fakeIdentityV3Service.identityFailureJsonRespTemplate)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/11111/",
                method: 'GET',
                headers: [
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ],
                defaultHandler: {
                    new Response(401, "", ["www-authenticate": "delegated"], "")
                }
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.findAll("WWW-Authenticate").size() == 1
        mc.receivedResponse.headers.findAll("WWW-Authenticate").get(0).matches("Keystone uri=.*")
    }
}
