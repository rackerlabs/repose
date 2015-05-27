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
package features.filters.keystonev2.cache

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class CacheTokenExpirationTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint

    MockIdentityService fakeIdentityService

    def setup() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')


    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    // D-13310 Repose should cache tokens with TTL's longer than MAX_INT for a duration of MAX_INT
    def "When Identity responds with a TTL > MAX_INT, Repose should cache for a duration of MAX_INT"() {

        given:
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(40);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        when: "I send a GET request to REPOSE with an X-Auth-Token header"
        fakeIdentityService.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityService.validateTokenCount == 1

        when: "I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeIdentityService.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        fakeIdentityService.validateTokenCount == 0

        when: "I troubleshoot the REPOSE logs"
        def foundLogs = reposeLogSearch.searchByString("Token TTL \\(" + clientToken + "\\) exceeds max expiration, setting to default max expiration")

        then: "I should have a WARN log message"
        foundLogs.size() == 1
        foundLogs[0].contains("WARN")
    }

}
