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
package features.filters.identityv3.delegating

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
 * Created by jennyvo on 11/17/14.
 * Identity V3 with Delegating option
 */
@Category(Filters)
class IdentityV3AuthNDelegatingTest extends ReposeValveTest {

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
        repose.configurationProvider.applyConfigs("features/filters/identityv3/delegating", params)
        repose.start()
        waitUntilReadyToServiceRequests('200')
    }

    def setup() {
        waitForHttpClientRequestCacheToClear()
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll("When #method req without credential")
    def "when send req without credential with delegating option repose forward req and failure msg to origin service"() {
        given:
        def delegatingmsg = "status_code=401.component=openstack-identity-v3.message=A subject token was not provided to validate;q=0.7"
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456/",
                method: method,
                headers: ['content-type': 'application/json'])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization")
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingmsg

        where:
        method << ["GET", "POST", "PUT", "PATCH", "DELETE"]
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
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        reqProject | authResponseCode | responseCode | responseBody                                          | delegatingMsg
        "p500"     | 401              | "200"        | "Unauthorized"                                        | "status_code=500.component=openstack-identity-v3.message=Valid admin token could not be fetched;q=0.7"
        "p501"     | 403              | "200"        | "Unauthorized"                                        | "status_code=500.component=openstack-identity-v3.message=Failed to validate subject token;q=0.7"
        "p502"     | 404              | "200"        | fakeIdentityV3Service.identityFailureJsonRespTemplate | "status_code=401.component=openstack-identity-v3.message=Failed to validate subject token;q=0.7"
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
