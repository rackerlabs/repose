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
import scaffold.category.Filters

@Category(Filters)
class IdentityV3Impersonation extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def setup() {
        fakeIdentityV3Service.resetParameters()
    }

    def "when requesting with impersonation, repose should add X-Impersonator-Name and X-Impersonator-Id"() {
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
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "repose should add X-Impersonator-Name and X-Impersonator-Id"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Name") == fakeIdentityV3Service.impersonate_name
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Id") == fakeIdentityV3Service.impersonate_id
    }

    def "when requesting without impersonation, repose should not add any impersonator headers"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_domainid = 789
            client_userid = 8910
        }

        when: "User passes a request through repose with no impersonation"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/789/",
                method: 'GET',
                headers: [
                        'content-type'   : 'application/json',
                        'X-Subject-Token': fakeIdentityV3Service.client_token,
                ]
        )

        then: "repose should not add any impersonator headers"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.contains("X-Impersonator-Name")
        !mc.handlings[0].request.headers.contains("X-Impersonator-Id")
    }
}
