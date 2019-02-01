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
package features.filters.experimental.tightlyCoupled

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

/**
 * Created by jennyvo on 5/16/14.
 * Test Custom filter (TighlyCoupled) with other filter (client-auth)
 *  Make sure still get modified response from custom filter
 */
@Category(Filters)
class TighlyCoupledandOtherFilterTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/tightlycoupledandotherfilter", params)

        deproxy = new Deproxy()
        repose.start(waitOnJmxAfterStarting: false)

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        waitUntilReadyToServiceRequests("200", false, true)
    }

    def "Proving that any other filter with a custom filter does in fact work"() {
        given:
        def body = "This should be the body"

        when:
        MessageChain mc = deproxy.makeRequest(
                method        : 'GET',
                url           : "$reposeEndpoint/get",
                headers       : ['X-Auth-Token': fakeIdentityService.client_token],
                defaultHandler: { new Response(200, null, null, body) })

        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body.contains(body)
        mc.receivedResponse.body.contains("<extra> Added by TestFilter, should also see the rest of the content </extra>")
        println(mc.receivedResponse.body)

        cleanup:
        repose?.stop()
        deproxy?.shutdown()
    }
}
