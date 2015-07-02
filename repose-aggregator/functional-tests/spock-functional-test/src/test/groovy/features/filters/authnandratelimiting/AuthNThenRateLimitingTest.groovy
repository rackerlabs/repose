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
package features.filters.authnandratelimiting

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared

class AuthNThenRateLimitingTest extends ReposeValveTest {
    @Shared
    def MockIdentityService fakeIdentityService

    def setupSpec() {
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/authnandratelimiting", params)
        repose.enableSuspend()
        repose.start()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    def "should be rate limited by the specific group matching x-pp-group value, and not the default group"() {
        given:
        fakeIdentityService.with {
            client_tenant = "6107362"
        }

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/v2/6107362/limits",
                headers: ["x-auth-token": fakeIdentityService.client_token])//, "x-pp-user": "coreywright;q=1.0", "x-pp-groups": "107;q=1.0"])
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2/6107362/limits",
                headers: ["x-auth-token": fakeIdentityService.client_token])

        then:
        mc.getReceivedResponse().getCode() == '200'
    }
}
