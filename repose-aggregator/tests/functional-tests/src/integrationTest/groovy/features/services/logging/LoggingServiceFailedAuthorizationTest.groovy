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
package features.services.logging


import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Services
import spock.lang.Shared

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

@Category(Services)
class LoggingServiceFailedAuthorizationTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    @Shared
    MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/logging/common", params)
        repose.configurationProvider.applyConfigs("features/services/logging/good", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
            'identity service', null, fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    def "Should still log even if auth-z fails"() {
        given: "Mock Identity with a random Tenant ID"
        fakeIdentityV2Service.with {
            client_tenantid = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
            client_username = UUID.randomUUID().toString()
            impersonate_id = UUID.randomUUID().toString()
            impersonate_name = UUID.randomUUID().toString()
        }

        when: "User sends a request to Repose with the wrong Tenant ID"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/authMe/",
            method: 'GET',
            headers: [
                'X-Auth-Token'     : fakeIdentityV2Service.client_token,
                'X-Expected-Tenant': UUID.randomUUID().toString()
            ]
        )

        then: "the Request should fail"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "the appropriate message logged"
        reposeLogSearch.awaitByString("INFO  user-log - userId=${fakeIdentityV2Service.client_userid} - userName=${fakeIdentityV2Service.client_username} - impersonatorUserId=${fakeIdentityV2Service.impersonate_id} - impersonatorUserName=${fakeIdentityV2Service.impersonate_name}")
    }
}
