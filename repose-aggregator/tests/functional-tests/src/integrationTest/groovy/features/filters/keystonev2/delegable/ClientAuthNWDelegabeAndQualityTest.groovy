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
package features.filters.keystonev2.delegable

import org.joda.time.DateTime
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/10/14.
 */
class ClientAuthNWDelegabeAndQualityTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/delegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)


    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    /*
    This test to verify the forward fail reason and default quality for authn
 */

    @Unroll("tenant: #requestTenant, response: #responseTenant, and #delegatedMsg")
    def "when req without token, non tenanted and delegable mode with quality"() {
        when:
        "User passes a request through repose with tenant in service admin role = $serviceAdminRole, request tenant: $requestTenant, response tenant: $responseTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json'])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-identity-status")
        request2.headers.getFirstValue("x-identity-status") == identityStatus
        request2.headers.contains("x-delegated")
        request2.headers.getFirstValue("x-delegated") =~ delegatedMsg

        where:
        requestTenant | responseTenant | serviceAdminRole | identityStatus  | delegatedMsg
        506           | 506            | "not-admin"      | "Indeterminate" | "status_code=401.component=keystone-v2.message=X-Auth-Token header not found;q=0.3"
        ""            | 512            | "not-admin"      | "Indeterminate" | "status_code=401.component=keystone-v2.message=X-Auth-Token header not found;q=0.3"
    }

    @Unroll("Req with auth resp: #authRespCode")
    def "When req with invalid token using delegable mode with quality"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
        }

        fakeIdentityV2Service.validateTokenHandler = {
            tokenId, tenantId, request ->
                new Response(authRespCode)
        }

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1234",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-identity-status")
        request2.headers.getFirstValue("x-identity-status") == "Indeterminate"
        request2.headers.contains("x-delegated")
        request2.headers.getFirstValue("x-delegated") =~ delegatedMsg

        and: "the datastore key for the token is sent"
        request2.headers.getFirstValue("X-Auth-Token-Key") == "IDENTITY:V2:TOKEN:${fakeIdentityV2Service.client_token}"

        where:
        authRespCode | delegatedMsg
        404          | "status_code=401`component=keystone-v2`message=Resource not found for validate token request;q=0.3" // REP-3212 changes
        401          | "status_code=500`component=keystone-v2`message=Admin token unauthorized to make validate token request;q=0.3"
    }
}

