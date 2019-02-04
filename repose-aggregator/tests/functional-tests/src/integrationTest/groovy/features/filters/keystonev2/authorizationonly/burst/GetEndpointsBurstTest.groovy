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
package features.filters.keystonev2.authorizationonly.burst

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Identity
import spock.lang.Unroll

@Category(Identity)
class GetEndpointsBurstTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()
        repose.configurationProvider.applyConfigs("common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorizationonly/common", properties.defaultTemplateParams)
        repose.start()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        fakeIdentityV2Service.originServicePort = properties.defaultTemplateParams.targetPort
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)
        Map header1 = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        def missingResponseErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-Auth-Token")) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING AUTH TOKEN")
            }
            return new Response(200, "OK", header1)
        }
        deproxy.defaultHandler = missingResponseErrorHandler
    }

    @Unroll("Testing with #numClients clients for #callsPerClient clients")
    def "under heavy load should not drop get endpoints response"() {
        given:
        Map header1 = ['X-Auth-Token': "$fakeIdentityV2Service.client_token-$numClients-$callsPerClient"]
        fakeIdentityV2Service.resetCounts()

        List<Thread> clientThreads = new ArrayList<Thread>()
        def missingAuthResponse = false
        def bad403Response = false

        (1..numClients).each {
            threadNum ->

                def thread = Thread.start {
                    (1..callsPerClient).each {
                        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: header1)

                        if (messageChain.receivedResponse.code.equalsIgnoreCase("502")) {
                            missingAuthResponse = true
                        }

                        if (messageChain.receivedResponse.code.equalsIgnoreCase("403")) {
                            bad403Response = true
                        }
                    }
                }
                clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        fakeIdentityV2Service.getEndpointsCount == 1

        and:
        !bad403Response

        and:
        !missingAuthResponse

        where:
        numClients | callsPerClient
        10         | 5
        20         | 10
        50         | 10
        100        | 5
    }
}
