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
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

/**
 * Created by jamesc on 12/2/14.
 * Update on 01/27/16
 *  - replace client-auth with keystone-v2 filter
 */
class DerpAndDelegableQuality extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/derp/responsemessaging/delegableQuality", params)
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
        This test to verify that discrete quality values for 2 delegable filters processed correctly by the
        DeRP filter.
    */
    @Unroll
    def "when req is #method to #path with roles \"#roles\" without token, then response should be #responseCode"() {
        given:
        fakeIdentityService.with {
            client_token = ""
            tokenExpiresAt = (new DateTime()).plusDays(1)
            service_admin_role = "non-admin"
        }
        Map<String, String> headers = ["X-Roles"     : roles,
                                       "Content-Type": "application/xml",
                                       "X-Auth-Token": fakeIdentityService.client_token]

        when: "User passes a request through repose with authN and apiValidator delegable"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/$path",
            method: method,
            headers: headers)

        then: "Origin Service should never be invoked and the Response from Repose to Client should be"
        mc.receivedResponse.code as Integer == responseCode
        mc.receivedResponse.message.contains(message)
        mc.handlings.size() == 0

        where:
        method   | path           | roles                 | responseCode          | message
        "GET"    | "servers/"     | "raxRole"             | SC_FORBIDDEN          | "forbidden"
        "GET"    | "servers/"     | "raxRole, a:observer" | SC_UNAUTHORIZED       | "X-Auth-Token header not found"
        "POST"   | "servers/1235" | "raxRole, a:observer" | SC_NOT_FOUND          | "Resource not found"
        "PUT"    | "servers/"     | "raxRole, a:admin"    | SC_METHOD_NOT_ALLOWED | "Bad method"
        "DELETE" | "servers/test" | "raxRole, a:observer" | SC_NOT_FOUND          | "Resource not found"
        "GET"    | "get/"         | "raxRole"             | SC_NOT_FOUND          | "Resource not found"
    }
}
