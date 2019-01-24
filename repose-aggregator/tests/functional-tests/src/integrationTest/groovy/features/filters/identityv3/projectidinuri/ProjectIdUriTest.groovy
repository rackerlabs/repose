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
package features.filters.identityv3.projectidinuri

import org.joda.time.DateTime
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class ProjectIdUriTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/projectidinuri/serviceroles", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def setup() {
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll("With project: #requestProject, response project: #responseProject, identity resp code (#authResponseCode), and group resp code (#groupResponseCode), return #responseCode")
    def "when authenticating project id - fail"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = responseProject
            service_admin_role = "not-admin"
            client_userid = requestProject
        }

        if (authResponseCode != 200) {
            fakeIdentityV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode)
            }
        }

        if (groupResponseCode != 200) {
            fakeIdentityV3Service.getGroupsHandler = {
                userId, request ->
                    new Response(groupResponseCode)
            }
        }

        when:
        "User passes a request through repose with request project: $requestProject, response project: $responseProject in a role that is not bypassed"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestProject/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("www-authenticate") == x_www_auth

        where:
        requestProject | responseProject | authResponseCode | groupResponseCode | x_www_auth | responseCode
        713            | 713             | 500              | 200               | false      | "500"
        714            | 714             | 404              | 200               | true       | "401"
        715            | 715             | 200              | 404               | false      | "500"
        716            | 716             | 200              | 500               | false      | "500"
        711            | 712             | 200              | 200               | true       | "401"
    }

    @Unroll("With Project ID: #requestProject, return from identity with response project: #responseProject, and role: #serviceAdminRole, return 200")
    def "when authenticating project id and roles that bypass"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = responseProject
            service_admin_role = serviceAdminRole
            client_userid = requestProject
        }

        when:
        "User passes a request through repose with request tenant: $requestProject, response tenant: $responseProject in a bypassed role = $serviceAdminRole"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestProject/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        //if projectId as tenantid in keystonev2 we expect should behave the same
        //then x-project-id should contain default project id - this isn't sure an issue
        //Comment this out for now will state in issue REP-3006
        //TODO - enable this after issue fix
        //request2.headers.getFirstValue("x-project-id") == responseProject.toString()
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == fakeIdentityV3Service.client_username + ";q=1.0"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy"
        request2.headers.getFirstValue("x-user-name") == "username"

        mc.receivedResponse.headers.contains("www-authenticate") == false

        where:
        requestProject | responseProject | serviceAdminRole      | responseCode
        717            | 717             | "not-admin"           | "200"
        718            | 719             | "service:admin-role1" | "200"
        719            | 720             | "service:admin-role2" | "200"
    }

    def "When project-id uri doesn't match identity response project no bypass roles"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = 12345
            service_admin_role = "non-admin"
            client_userid = 1234
        }

        when:
        "User passes a request through repose with request project not matching"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/999/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.handlings.size() == 0
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders) }
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_projectid = 123
            client_userid = 123
        }

        when: "User passes a request through repose"
        def mc =
                deproxy.makeRequest(
                        url: reposeEndpoint + "/servers/123/",
                        method: 'GET',
                        headers: ['content-type': 'application/json', 'X-Subject-Token': fakeIdentityV3Service.client_token],
                        defaultHandler: xmlResp
                )

        then:
        mc.receivedResponse.code == "201"
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
