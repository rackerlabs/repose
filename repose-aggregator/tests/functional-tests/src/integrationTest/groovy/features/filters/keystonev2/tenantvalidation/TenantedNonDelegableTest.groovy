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
import spock.lang.Ignore
import spock.lang.Unroll

@Category(Filters)
class TenantedNonDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant/tenantednondelegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)


    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    @Unroll("tenant: #requestTenant, with return from identity with HTTP code (#authResponseCode), group HTTP code (#groupResponseCode) and response tenant: #responseTenant")
    def "when authenticating user in tenanted and non delegable mode - fail scenarios"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenantid = responseTenant
            service_admin_role = "not-admin"
            client_userid = requestTenant
        }

        if (authResponseCode != 200) {
            fakeIdentityV2Service.validateTokenHandler = {
                tokenId, tenantId, request ->
                    new Response(authResponseCode)
            }
        }

        if (groupResponseCode != 200) {
            fakeIdentityV2Service.getGroupsHandler = {
                userId, request ->
                    new Response(groupResponseCode)
            }
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in non-admin service role"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("www-authenticate") == x_www_auth

        where:
        requestTenant | responseTenant | authResponseCode | responseCode | groupResponseCode | x_www_auth
        713           | 713            | 500              | "502"        | 200               | false
        714           | 714            | 404              | "401"        | 200               | true
        715           | 715            | 200              | "200"        | 404               | false     // REP-3212 changes
        716           | 716            | 200              | "502"        | 500               | false
        711           | 712            | 200              | "401"        | 200               | true
    }

    /**
     * this tests the negative scenarios in tenanted and non-delegable mode with no groups
     * - token validation fails (500)
     * - tenant id in the request does not match tenant id in the response from identity and service admin role is not present
     * - token is not found (404)
     * - get groups call responds with 500
     * - get groups call returns with a 404
     * - token returns expired
     * @return
     */
    @Unroll("tenant: #requestTenant, with return from identity with response tenant: #responseTenant and role: #serviceAdminRole")
    def "when authenticating user in tenanted and non delegable mode - success"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenantid = responseTenant
            client_tenantname = responseTenant
            service_admin_role = serviceAdminRole
            client_userid = requestTenant
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in service admin role = $serviceAdminRole"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "DFW"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-tenant-name") == responseTenant.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == "username"
        request2.headers.contains("x-roles")

        request2.headers.getFirstValue("x-authorization") == "Proxy $responseTenant"
        request2.headers.getFirstValue("x-user-name") == "username"

        !mc.receivedResponse.headers.contains("www-authenticate")

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode
        717           | 717            | "not-admin"           | "200"
        718           | 719            | "service:admin-role1" | "200"
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders) }
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = 123
            client_tenantid = 123
        }

        when: "User passes a request through repose"
        def mc =
                deproxy.makeRequest(
                        url: reposeEndpoint + "/servers/123/",
                        method: 'GET',
                        headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token],
                        defaultHandler: xmlResp
                )

        then:
        mc.receivedResponse.code == "201"
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    // REP-2670: Ded Auth Changes
    def "Always add x-tenant to request for origin service use"() {
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "456"
            client_tenantid = "456"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/456", method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Things are forward to the origin, because we're not validating existence of tenant"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.getHandlings().get(0).getRequest().getHeaders().contains("x-tenant-id")
        mc.getHandlings().get(0).getRequest().getHeaders().contains("x-tenant-name")
    }

    // REP-2670: Ded Auth Changes
    @Unroll("Request Tenant: #requestTenant")
    def "URI tenant will not get added to x-tenant"() {
        given: "identity info"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = 123
            client_tenantid = 12345
            client_tenantid2 = "nast-id"
        }

        when: "pass request with request tenant"
        def mc =
                deproxy.makeRequest(
                        url: reposeEndpoint + "/servers/" + requestTenant,
                        method: 'GET',
                        headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token]
                )

        then: "should satisfy the following"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.getHandlings().get(0).getRequest().getHeaders().findAll("x-tenant-id").get(0).split(",").size() == 1
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-tenant-id") == "12345" // Default tenant

        where:
        requestTenant << ["12345", "nast-it"]
    }

    // REP-2670: Ded Auth Changes -Zerotenant
    def "Non-tenant(racker) with tenanted mode and bypass service admin role"() {
        fakeIdentityV2Service.with {
            client_token = "rackerSSO"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "rackerSSOUsername"
            service_admin_role = "service:admin-role1"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/456/", method: 'GET',
                headers: ['content-type': 'application/json',
                          'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Things are forward to the origin, because we're not validating tenant from uri"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    // REP-2670: Ded Auth Changes
    @Unroll("tenant: #requestTenant, with return from identity default tenant: #responseTenant")
    def "authenticate user in tenanted and non delegable mode with requestTenant - success"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenantid = responseTenant
            client_tenantname = responseTenant
            service_admin_role = serviceAdminRole
            client_tenantid2 = requestTenant
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in service admin role = $serviceAdminRole"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "DFW"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-tenant-name") == responseTenant.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == "username"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy $requestTenant"
        request2.headers.getFirstValue("x-user-name") == "username"

        !mc.receivedResponse.headers.contains("www-authenticate")

        where:
        requestTenant | responseTenant | serviceAdminRole | responseCode
        717           | 717            | "not-admin"      | "200"
        718           | 719            | "not-admin"      | "200"
    }

    // REP-2670: Ded Auth Changes
    // Currently, without a default tenantID, we do not make the Valkyrie call.
    // We will remove the requirement for a default tenantID so that when we don’t have a default URI,
    // we will rely on a tenantID from the validate token call
    // apply for this case dedicated user
    def "Remove reliance on default tenant check"() {
        given: "keystone v2v2 with dedicated user access"
        def hybridtenant = "hybrid:12345"
        fakeIdentityV2Service.with {
            client_token = "dedicatedUser"
            client_userid = "dedicatedUser"
            client_tenantid = hybridtenant
        }
        when:
        "User passes a request through repose with request tenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/" + hybridtenant,
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )
        then: "Things are forward to the origin, because we're not validating existence of tenant"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.getHandlings().get(0).getRequest().getHeaders().contains("x-tenant-id")
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-tenant-id") == hybridtenant
    }
}
