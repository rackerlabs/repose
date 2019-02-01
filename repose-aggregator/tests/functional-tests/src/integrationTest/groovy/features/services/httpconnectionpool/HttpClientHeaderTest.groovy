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
package features.services.httpconnectionpool

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services

/**
 * Test to verify that headers are added to requests made using an HTTP connection pool configured to do so.
 */
@Category(Services)
class HttpClientHeaderTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/headers", params)

        deproxy = new Deproxy()
        repose.start(waitOnJmxAfterStarting: false)

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(
                properties.identityPort, 'identity service', null, fakeIdentityService.handler)

        waitUntilReadyToServiceRequests("200", false, true)
    }

    def "configured connection pool headers are added to requests using that pool"() {
        when:
        MessageChain mc = deproxy.makeRequest(
                method : 'GET',
                url    : "$reposeEndpoint/get",
                headers: ['X-Auth-Token': fakeIdentityService.client_token, fancy: 'pants'])

        then: 'request to identity has the configured connection pool headers'
        mc.receivedResponse.code == '200'
        mc.orphanedHandlings[0].request.headers.getFirstValue('butts') == 'pandas'
        !mc.orphanedHandlings[0].request.headers.getFirstValue('fancy')

        and: 'origin service received the correct headers'
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue('fancy') == 'pants'
        !mc.handlings[0].request.headers.getFirstValue('butts')
    }
}
