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
package features.filters.derp

import org.joda.time.DateTime
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

/**
 * Created by jamesc on 12/1/14.
 * Update on 01/27/16
 *  - replace client-auth with keystone-v2 filter
 */
class DerpAndClientAuthNDelegable extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/derp/responsemessaging/keystonev2", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
            'identity service', null, fakeIdentityService.handler)


    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    /*
        These tests are to verify the delegation of authn failures to the derp filter, which then forwards
        that information back to the client.  The origin service, thus, never gets invoked.
    */

    @Unroll
    def "Req without token and tenant \"#requestTenant\" should return UNAUTHORIZED (401)"() {
        given:
        fakeIdentityService.with {
            client_token = ""
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_tenantid = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when:
        "User passes a request through repose with tenant in service admin role = $serviceAdminRole, request tenant: $requestTenant, response tenant: $responseTenant"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/servers/$requestTenant",
            method: 'GET',
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Origin Service should never be invoked and the Response from Repose to Client should be"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.receivedResponse.message.contains("X-Auth-Token header not found")
        mc.handlings.size() == 0

        where:
        requestTenant | responseTenant | serviceAdminRole
        506           | 506            | "not-admin"
        ""            | 512            | "not-admin"
    }


    @Unroll
    def "Req with auth resp: #authRespCode using delegable mode with quality should return #responseCode"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        fakeIdentityService.validateTokenHandler = {
            tokenId, tenantid, request ->
                new Response(authRespCode)
        }

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/servers/1234",
            method: 'GET',
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Origin Service should never be invoked and the Response from Repose to Client should be"
        mc.receivedResponse.code as Integer == responseCode
        mc.receivedResponse.message.contains(message)
        mc.handlings.size() == 0

        where:
        authRespCode | responseCode             | message
        404          | SC_UNAUTHORIZED          | "Resource not found for validate token request"
        401          | SC_INTERNAL_SERVER_ERROR | "Admin token unauthorized to make validate token request"
    }
}
