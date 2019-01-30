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
package features.filters.keystonev2.ignoredroles

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID

@Category(Filters)
class Keystonev2IgnoredRolesDefaultTest extends ReposeValveTest {

    static final String DEFAULT_IGNORED_ROLE_NAME = 'identity:tenant-access'

    static MockIdentityV2Service mockIdentityV2Service

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()

        mockIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(name: 'Origin Service', port: params.targetPort)
        deproxy.addEndpoint(name: 'Identity Service', port: params.identityPort, defaultHandler: mockIdentityV2Service.handler)

        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/ignoredroles/common", params)
        repose.configurationProvider.applyConfigs('features/filters/keystonev2/ignoredroles/default', params)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        // Reset the mock Identity service
        mockIdentityV2Service.resetDefaultParameters()
        mockIdentityV2Service.resetCounts()
        mockIdentityV2Service.resetHandlers()
        mockIdentityV2Service.tokenExpiresAt = null
        mockIdentityV2Service.checkTokenValid = false
    }

    def 'the ignored role should be excluded from the roles header'() {
        given:
        List<String> notIgnoredroles = ['service1:admin', 'service2:observer']
        mockIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            validateTokenHandler = createValidateTokenHandler(
                roles: [
                    [name: DEFAULT_IGNORED_ROLE_NAME],
                    [name: 'service1:admin'],
                    [name: 'service2:observer'],
                    [name: DEFAULT_IGNORED_ROLE_NAME]
                ]
            )
        }

        when: 'a request with a valid token is made to Repose'
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(AUTH_TOKEN): mockIdentityV2Service.client_token])

        then: 'the request should pass to the origin service'
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: 'the roles header is fetched and normalized'
        String roles = messageChain.handlings[0].request.headers.findAll(ROLES).inject { String acc, String val ->
            ",".join(acc, val)
        }

        then: 'the roles header should not contain the ignored role'
        !roles.contains(DEFAULT_IGNORED_ROLE_NAME)

        and: 'the roles header should contain the other roles'
        roles.contains('service1:admin')
        roles.contains('service2:observer')
    }

    def 'tenants only presenting the ignored role should be excluded from the tenants header'() {
        given:
        String ignoredTenantId = 'potentialTenantId'
        mockIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            validateTokenHandler = createValidateTokenHandler(
                roles: [
                    [name: DEFAULT_IGNORED_ROLE_NAME, tenantId: ignoredTenantId],
                    [name: 'service1:admin', tenantId: 'service1:tenant'],
                    [name: 'service2:observer']
                ]
            )
        }

        when: 'a request with a valid token is made to Repose'
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(AUTH_TOKEN): mockIdentityV2Service.client_token])

        then: 'the request should pass to the origin service'
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: 'the tenants header is fetched and normalized'
        String tenants = messageChain.handlings[0].request.headers.findAll(TENANT_ID).inject { String acc, String val ->
            ",".join(acc, val)
        }

        then: 'the tenants header should not contain the ignored tenant'
        !tenants.contains(ignoredTenantId)

        and: 'the tenants header should contain the other tenants'
        tenants.contains('service1:tenant')
    }

    def 'tenants presenting the ignored role and other roles should be included in the tenants header'() {
        given:
        String tenantId = 'aTenantId'
        mockIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            validateTokenHandler = createValidateTokenHandler(
                roles: [
                    [name: DEFAULT_IGNORED_ROLE_NAME, tenantId: tenantId],
                    [name: 'service1:admin', tenantId: tenantId],
                    [name: 'service2:observer']
                ]
            )
        }

        when: 'a request with a valid token is made to Repose'
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: [(AUTH_TOKEN): mockIdentityV2Service.client_token])

        then: 'the request should pass to the origin service'
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: 'the tenants header is fetched and normalized'
        String tenants = messageChain.handlings[0].request.headers.findAll(TENANT_ID).inject { String acc, String val ->
            ",".join(acc, val)
        }

        then: 'the tenants header should contain the tenant'
        tenants.contains(tenantId)
    }

    Closure<Response> createValidateTokenHandler(Map params = [:]) {
        { String tokenId, String tenantId, Request request ->
            def headers = [(CONTENT_TYPE): APPLICATION_JSON]
            def body = mockIdentityV2Service.createAccessJsonWithValues(params)
            new Response(SC_OK, null, headers, body)
        }
    }
}
