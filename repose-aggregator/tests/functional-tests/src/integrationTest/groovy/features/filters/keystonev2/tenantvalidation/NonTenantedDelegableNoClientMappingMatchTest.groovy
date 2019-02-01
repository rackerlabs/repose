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
package features.filters.keystonev2.tenantvalidation

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class NonTenantedDelegableNoClientMappingMatchTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant/nontenanteddelegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)


    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }


    @Unroll("tenant: #requestTenant, with return from identity with status code #authResponseCode and response tenant: #responseTenant")
    def "when authenticating user in non tenanted with delegable mode whatever failure for auth response will be forwarded to origin service"() {
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenantid = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        if (authResponseCode != 200) {
            fakeIdentityV2Service.validateTokenHandler = {
                tokenId, tenantId, request ->
                    new Response(authResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint//servers/$requestTenant/",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        requestTenant | responseTenant | serviceAdminRole | authResponseCode | delegatingMsg
        400           | 401            | "not-admin"      | 500              | "status_code=502"
        402           | 403            | "not-admin"      | 404              | "status_code=401"

    }


    @Unroll("tenant: #requestTenant, with return from identity with response tenant: #responseTenant, token: #clientToken, and role: #serviceAdminRole")
    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching - pass"() {

        fakeIdentityV2Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenantid = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint//servers/$requestTenant/",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == default_region
        request2.headers.contains("x-identity-status")
        request2.headers.getFirstValue("x-identity-status") == identityStatus

        if (identityStatus == "Indeterminate") {
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("x-authorization") == "Proxy $responseTenant"
        }

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode | identityStatus  | clientToken       | default_region
        404           | 405            | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID() | "DFW"
        406           | 406            | "not-admin"           | "200"        | "Indeterminate" | ""                | null
        407           | 407            | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID() | "DFW"
        408           | 408            | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID() | "DFW"
        409           | 410            | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID() | "DFW"
        ""            | 412            | "not-admin"           | "200"        | "Indeterminate" | ""                | null

    }

}
