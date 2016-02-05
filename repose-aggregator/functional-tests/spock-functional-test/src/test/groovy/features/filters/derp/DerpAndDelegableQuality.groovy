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

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

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

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
        if (repose)
            repose.stop()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    /*
        This test to verify that discrete quality values for 2 delegable filters processed correctly by the
        DeRP filter.
    */

    @Unroll("req method: #method, #path, #roles")
    def "when req without token, non tenanted and delegable mode (2) with quality"() {
        given:
        fakeIdentityService.with {
            client_token = ""
            tokenExpiresAt = (new DateTime()).plusDays(1);
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

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0


        where:
        method   | path           | roles                 | responseCode | msgBody                         | component       | quality
        "GET"    | "servers/"     | "raxRole"             | "403"        | "forbidden"                     | "api-validator" | 0.6
        "GET"    | "servers/"     | "raxRole, a:observer" | "401"        | "X-Auth-Token header not found" | "keystone-v2"   | 0.3
        "POST"   | "servers/1235" | "raxRole, a:observer" | "404"        | "Resource not found"            | "api-validator" | 0.6
        "PUT"    | "servers/"     | "raxRole, a:admin"    | "405"        | "Bad method"                    | "api-validator" | 0.6
        "DELETE" | "servers/test" | "raxRole, a:observer" | "404"        | "Resource not found"            | "api-validator" | 0.6
        "GET"    | "get/"         | "raxRole"             | "404"        | "Resource not found"            | "api-validator" | 0.6

    }

}

