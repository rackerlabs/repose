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
import spock.lang.Ignore

/**
 * Created by jennyvo on 9/4/14.
 * test token max expired
 */
@Ignore("Ignore this test for now since we haven't explicitly logged the WARN message to client")
@Category(Filters)
class IdentityV3CacheTokenExpirationTest extends ReposeValveTest {
    def originEndpoint
    def identityEndpoint

    MockIdentityV3Service fakeIdentityV3Service

    def setup() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("/features/filters/identityv3", params)
        repose.configurationProvider.applyConfigs("/features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("/features/filters/identityv3/connectionpooling", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def "When Identity responds with a TTL > MAX_INT, Repose should cache for a duration of MAX_INT"() {

        given:
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.client_token = clientToken
        fakeIdentityV3Service.tokenExpiresAt = (new DateTime()).plusDays(40);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        when: "I send a GET request to REPOSE with an X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 1

        when: "I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        fakeIdentityV3Service.validateTokenCount == 0

        when: "I troubleshoot the REPOSE logs"
        def foundLogs = reposeLogSearch.searchByString("Token TTL \\(" + clientToken + "\\) exceeds max expiration, setting to default max expiration")

        then: "I should have a WARN log message"
        foundLogs.size() == 1
        foundLogs[0].contains("WARN")
    }
}
