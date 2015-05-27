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

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class ClientAuthNTenantedDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystone-v2/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/keystone-v2/removetenant/tenanteddelegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }


    @Unroll("tenant: #requestTenant, with return from identity with HTTP code (#authResponseCode) response tenant: #responseTenant, token: #clientToken")
    def "when authenticating user in tenanted and delegable mode any failures will be forward to origin service with desc msg"() {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = "not-admin"
        }

        if (authResponseCode != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(authResponseCode)
            }
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-delegated")
        request2.headers.getFirstValue("x-delegated") =~ delegatedMsg


        where:
        requestTenant | responseTenant | authResponseCode | responseCode | clientToken       | delegatedMsg
        300           | 301            | 500              | "200"        | UUID.randomUUID() | "status_code=500.component=client-auth-n.message=.*;q=0.7"
        302           | 303            | 404              | "200"        | UUID.randomUUID() | "status_code=401.component=client-auth-n.message=.*;q=0.7"
        304           | 305            | 200              | "200"        | UUID.randomUUID() | "status_code=401.component=client-auth-n.message=.*;q=0.7"
        306           | 306            | 200              | "200"        | ""                | "status_code=401.component=client-auth-n.message=.*;q=0.7"

    }


    @Unroll("tenant: #requestTenant, with return from identity with response tenant: #responseTenant, token: #clientToken, and role: #serviceAdminRole")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching - pass"() {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        and: "If request made it to origin service"
        if (identityStatus == "Confirmed") {
            def request2 = mc.handlings[0].request
            assert (mc.handlings[0].endpoint == originEndpoint)
            assert (request2.headers.contains("x-auth-token"))
            assert (request2.headers.contains("x-identity-status"))
            assert (request2.headers.contains("x-authorization"))
            assert (request2.headers.getFirstValue("x-identity-status") == identityStatus)
            assert (request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        and: "If identityStatus was Indeterminate"
        if (identityStatus == "Indeterminate") {

            def request2 = mc.handlings[0].request
            assert (request2.headers.getFirstValue("x-identity-status") == identityStatus)
            assert (request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode | identityStatus  | clientToken
        307           | 307            | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID()
        308           | 308            | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()
        309           | 310            | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()
        ""            | 312            | "not-admin"           | "200"        | "Indeterminate" | ""
    }

    def "when authenticating user in tenanted and delegable mode verify origin response code does not change"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = 320
            client_userid = 320
            service_admin_role = "not-admin"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/330",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token],
                defaultHandler: { return new Response(302, "Redirect") })

        then: "Response code does not change"
        mc.receivedResponse.code == "302"
        mc.handlings.size() == 1

        and: "If request made it to origin service"
        def request2 = mc.handlings[0].request
        assert (mc.handlings[0].endpoint == originEndpoint)
        assert (request2.headers.contains("x-auth-token"))
        assert (request2.headers.contains("x-identity-status"))
        assert (request2.headers.contains("x-authorization"))
        assert (request2.headers.getFirstValue("x-identity-status") == "Indeterminate")
        assert (request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))

    }

    /*
        This test verify delegated message to service origin with tenanted
     */

    @Unroll("Tenant: #requestTenant, response #responseTenant and #delegatedMsg")
    def "When req with tenanted config fail delegable option forward failed message to origin"() {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        and:
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("x-identity-status") == identityStatus
        request2.headers.getFirstValue("x-authorization").startsWith("Proxy")
        request2.headers.contains("x-delegated")
        request2.headers.getFirstValue("x-delegated") =~ delegatedMsg

        where:
        requestTenant | responseTenant | serviceAdminRole | identityStatus  | clientToken       | delegatedMsg
        309           | 310            | "non-admin"      | "Indeterminate" | UUID.randomUUID() | "status_code=401.component=client-auth-n.message=Unable to validate token for tenant. Invalid token:\\s.*;q=0.7"
        ""            | 312            | "not-admin"      | "Indeterminate" | ""                | "status_code=401.component=client-auth-n.message=Failure in Auth-N filter.;q=0.7"
    }


}
