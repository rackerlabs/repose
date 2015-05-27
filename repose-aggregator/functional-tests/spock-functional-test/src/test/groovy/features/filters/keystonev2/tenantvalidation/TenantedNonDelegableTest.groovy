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

class TenantedNonDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/tenantednondelegable", params)
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

    @Unroll("tenant: #requestTenant, with return from identity with HTTP code (#authResponseCode), group HTTP code (#groupResponseCode) and response tenant: #responseTenant")
    def "when authenticating user in tenanted and non delegable mode - fail scenarios"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = "not-admin"
            client_userid = requestTenant
        }

        if (authResponseCode != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(authResponseCode)
            }
        }

        if (groupResponseCode != 200) {
            fakeIdentityService.getGroupsHandler = {
                userId, request, xml ->
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
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("www-authenticate") == x_www_auth

        where:
        requestTenant | responseTenant | authResponseCode | responseCode | groupResponseCode | x_www_auth
        713           | 713            | 500              | "500"        | 200               | false
        714           | 714            | 404              | "401"        | 200               | true
        715           | 715            | 200              | "500"        | 404               | false
        716           | 716            | 200              | "500"        | 500               | false
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
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
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
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-tenant-name") == responseTenant.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == "username;q=1.0"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy $requestTenant"
        request2.headers.getFirstValue("x-user-name") == "username"

        mc.receivedResponse.headers.contains("www-authenticate") == false

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode
        717           | 717            | "not-admin"           | "200"
        718           | 719            | "service:admin-role1" | "200"
    }

    def "Should split request headers according to rfc by default"() {
        given:
        def reqHeaders = ["user-agent"                                                                 : "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36", "x-pp-user": "usertest1," +
                "usertest2, usertest3", "accept"                                                       : "application/xml;q=1 , application/json;q=0.5"]
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 720
            client_userid = 720
        }

        when: "User passes a request through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/123/", method: 'GET', headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token] + reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("user-agent") == 1
        mc.handlings[0].request.headers.getCountByName("x-pp-user") == 4
        mc.handlings[0].request.headers.getCountByName("accept") == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders) }
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = 123
        }

        when: "User passes a request through repose"
        def mc =
                deproxy.makeRequest(
                        url: reposeEndpoint + "/servers/123/",
                        method: 'GET',
                        headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token],
                        defaultHandler: xmlResp
                )

        then:
        mc.receivedResponse.code == "201"
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers.findAll("via").size() == 1
    }


}
