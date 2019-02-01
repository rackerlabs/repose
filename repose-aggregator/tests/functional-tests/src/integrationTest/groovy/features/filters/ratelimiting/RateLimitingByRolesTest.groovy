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
package features.filters.ratelimiting

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Ignore

/**
 * Created by jennyvo on 7/2/15.
 * Test to prove that rate limit on role after using header translation to x-pp-groups
 */
@Category(Filters)
class RateLimitingByRolesTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/ratelimitbyroletest", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def "Test ratelimit on x-roles with translate header to x-pp-groups"() {
        given:
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = reqDomain
            client_userid = reqUserId
            service_admin_role = "test-admin"
        }

        when: "User passes a request through repose and the rate-limit has not been reached"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-roles")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Secure Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("test-admin")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("member")

        when: "the user hit the rate-limit"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqDomain/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "Request should be ratelimit"
        mc.receivedResponse.code == "413"
    }

    def "Test will not ratelimit when set different roles"() {
        given:
        fakeIdentityV3Service.resetParameters()
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusSeconds(2)
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
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-roles")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("service:admin-role1")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("member")
    }

    def "Verify header translation copy all header values not just the first" () {
        given:
        fakeIdentityV3Service.resetParameters()
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusSeconds(2)
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
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-roles")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Secure Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("service:admin-role1")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("member")
        (mc.handlings[0].request.headers.findAll("something").toString()).split(",").size() == 5

    }

    @Ignore("This could be a bug but need to discuss to see if we keep current auth behavior")
    def "Verify header translation copy all header values from request" () {
        given:
        fakeIdentityV3Service.resetParameters()
        def reqDomain = fakeIdentityV3Service.client_domainid
        def reqUserId = fakeIdentityV3Service.client_userid

        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusSeconds(2)
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
                        'x-roles':'test',
                        'X-Roles':'user'
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-roles")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("Secure Developers")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("service:admin-role1")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("member")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("test")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("user")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("test")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("user")
    }
}
