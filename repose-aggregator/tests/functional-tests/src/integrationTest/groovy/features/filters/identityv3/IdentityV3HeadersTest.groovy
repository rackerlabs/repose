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
import spock.lang.Ignore

/**
 * Created by jennyvo on 8/26/14.
 */
@Category(Filters)
class IdentityV3HeadersTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def setup() {
        fakeIdentityV3Service.resetCounts()
        fakeIdentityV3Service.resetHandlers()
        fakeIdentityV3Service.resetParameters()
    }

    def "When token is validated, set of headers should be generated"() {
        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityV3Service.default_region = "DFW"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should validate the token and path the user's default region as the X-Default_Region header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityV3Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Default-Region")
        request.headers.contains("X-Authorization")
        request.headers.contains("X-Project-Id")
        request.headers.contains("X-Project-Name")
        request.headers.contains("X-User-Id")
        request.headers.contains("X-User-Name")
        request.headers.contains("X-Roles")
        request.headers.contains("X-pp-user")
        request.headers.contains("X-pp-groups")
        request.headers.contains("X-Token-Expires")
        request.headers.contains("X-Catalog")
        request.headers.getFirstValue("X-Default-Region") == "DFW"
        request.headers.findAll("X-PP-Groups").containsAll(["Developers;q=1.0", "Repose Developers;q=1.0", "Secure Developers;q=1.0"])
        new String(Base64.getDecoder().decode(request.headers.getFirstValue("X-Catalog"))) ==~
                /\[\{"endpoints":\[\{"id":"39dc322ce86c4111b4f06c2eeae0841b","interface":"public","region":"RegionOne","url":"http:\/\/localhost:\d+"\},\{"id":"ec642f27474842e78bf059f6c48f4e99","interface":"internal","region":"RegionOne","url":"http:\/\/localhost:\d+"\},\{"id":"c609fc430175452290b62a4242e8a7e8","interface":"admin","region":"RegionOne","url":"http:\/\/localhost:\d+"\}\],"id":"4363ae44bdf34a3981fde3b823cb9aa2","type":"identity","name":"keystone"\}\]/

        when: "I send a second GET request to Repose with the same token"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-Default-Region header"
        mc.receivedResponse.code == "200"
        fakeIdentityV3Service.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "DFW"
    }

    def "when client failed to authenticate, the XXX-Authentication header should be expected"() {
        given:
        fakeIdentityV3Service.with {
            client_domainid = 11111
            client_userid = 11111
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                new Response(404, null, null, fakeIdentityV3Service.identityFailureAuthJsonRespTemplate)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/11111/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.getFirstValue("WWW-Authenticate") == "Keystone uri=http://" + identityEndpoint.hostname + ":" + properties.identityPort
    }

    def "Verify auth filter adding headers not replace headers"() {
        given:
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = reqDomain
            client_userid = reqUserId
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                        'x-roles'        : 'test',
                        'X-Roles'        : 'user',
                        'x-pp-groups'    : 'Repose Test',
                        'x-pp-user'      : 'Repose user'
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Secure Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Repose Test")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("service:admin-role1")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("member")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("test")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("user")
        (mc.handlings[0].request.headers.findAll("x-pp-user").toString()).contains("Repose user")
        (mc.handlings[0].request.headers.findAll("x-pp-user").toString()).contains("username;q=1.0")
    }

    @Ignore ("We can turn on when impersonator role to header merge in to branch")
    def "Verify with impersonation, repose should add x-impersonator-roles headers"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = 12345
            client_userid = 123456
            impersonate_name = "impersonator_name"
            impersonate_id = "567"
        }

        when: "User passes a request with impersonation through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/12345/",
                method: 'GET',
                headers: [
                        'content-type'        : 'application/json',
                        'X-Subject-Token'     : fakeIdentityV3Service.client_token,
                        'x-impersonator-roles': 'repose-test'
                ]
        )

        then: "repose should add X-Impersonator-Name and X-Impersonator-Id"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Name") == fakeIdentityV3Service.impersonate_name
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Id") == fakeIdentityV3Service.impersonate_id
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("Racker")
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("object-store:admin")
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("repose-test")
    }
}
