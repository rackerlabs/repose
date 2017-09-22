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
package features.recipes.tenantCullingRbac

import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Shared

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

class TenantCullingRBACRecipeCacheTest extends ReposeValveTest {
    @Shared
    MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.identityPort, name: 'identity service', defaultHandler: fakeIdentityService.handler)
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/recipes/tenantCullingRbac/common', params)
        repose.configurationProvider.applyConfigs('features/recipes/tenantCullingRbac/cache', params)
        repose.start()
    }

    def setup() {
        // Reset the mock Identity service
        fakeIdentityService.resetDefaultParameters()
        fakeIdentityService.resetCounts()
        fakeIdentityService.resetHandlers()
        fakeIdentityService.tokenExpiresAt = null
        fakeIdentityService.checkTokenValid = false

        // Change the client token for each test so that the cached data in the Akka Client is not used.
        fakeIdentityService.client_token = UUID.randomUUID().toString()
    }

    def "the user should receive a 401 response if their token was evicted from the cache prior to tenant culling"() {
        given:
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler()

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            path: '/open',
            headers: [
                'X-Auth-Token': fakeIdentityService.client_token
            ]
        )

        then: 'the user request does not reach the origin service'
        messageChain.handlings.size() == 0
        messageChain.receivedResponse.code as Integer == SC_UNAUTHORIZED
    }

    Closure<Response> createValidateTokenHandler(Map params = [:]) {
        { String tokenId, String tenantId, Request request ->
            def headers = [(CONTENT_TYPE): APPLICATION_JSON]
            def body = fakeIdentityService.createAccessJsonWithValues(params)
            new Response(SC_OK, null, headers, body)
        }
    }
}
