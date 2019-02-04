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
package features.filters.keystonev2.selfvalidation

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Identity
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

/**
 * Created by jennyvo on 9/22/15.
 */
@Category(Identity)
class KeystoneV2SelfValidationTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/selfvalidation", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetCounts()
        fakeIdentityV2Service.resetDefaultParameters()
        fakeIdentityV2Service.resetHandlers()
        sleep(500)
    }

    def "Validate client token test"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
        }

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "They should pass"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-tenant-id") == "mytenant"
        mc.getHandlings().get(0).getRequest().getHeaders().getFirstValue("x-tenant-name") == "mytenantname"

        and: "The X-Authenticated-By headers should be present at the origin service."
        mc.getHandlings().get(0).getRequest().getHeaders().findAll("X-Authenticated-By").sort() == ["PASSCODE", "PASSWORD"]
    }

    def "Validate client token with belongsTo test"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            validateTenant = "belongstotest"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "They should pass"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "Validate client token fails"() {
        given:
        fakeIdentityV2Service.with {
            isTokenValid = false
        }

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "They should fail"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED // 401
        mc.handlings.size() == 0
    }

    def "Validate racker token without tenant"() {
        given:
        fakeIdentityV2Service.with {
            client_token = "rackerSSO"
            client_userid = "rackerSSOUsername"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "They should pass"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    @Unroll("When pass request with white list uri: #path")
    def "Verify white list"() {
        given:

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: 'GET')

        then: "They should pass"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        where:
        path << ["/buildinfo", "/get"]
    }

    def "Verify Repose send Impersonate role in header"() {
        given: "keystone v2v2 with impersonate access"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            impersonate_id = "12345"
            impersonate_name = "repose_test"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should have x-impersonate-roles in headers from request come through repose"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Name") == fakeIdentityV2Service.impersonate_name
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Id") == fakeIdentityV2Service.impersonate_id
        mc.handlings[0].request.headers.contains("x-impersonator-roles")
        // should check if take roles id or role name???
        mc.handlings[0].request.headers.getFirstValue("x-impersonator-roles").contains("Racker")
        mc.handlings[0].request.headers.getFirstValue("x-impersonator-roles").contains("object-store:admin")
    }

    def "If no impersonator then no impersonator headers"() {
        given: "keystone v2v2 without impersonate access"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should have x-impersonate-roles in headers from request come through repose"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.contains("x-impersonator-id")
        !mc.handlings[0].request.headers.contains("x-impersonator-name")
        !mc.handlings[0].request.headers.contains("x-impersonator-roles")
    }

    def "when Identity returns a 401 a validate token call, Repose should return a 401"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            validateTokenHandler = { tokenId, tenantId, request ->
                new Response(401)
            }
        }

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then:
        mc.receivedResponse.code.toInteger() == 401
        mc.receivedResponse.headers.contains("www-authenticate")
        mc.handlings.size() == 0
    }

    def "when Identity returns a 401 on a get groups call, Repose should return a 401"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            getGroupsHandler = { userId, request ->
                new Response(401)
            }
        }

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then:
        mc.receivedResponse.code.toInteger() == 401
        mc.receivedResponse.headers.contains("www-authenticate")
        mc.handlings.size() == 0
    }
}
