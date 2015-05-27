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
package features.filters.keystonev2.traceheader

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/22/15.
 *  Tracing header should include in request from Repose to services as Identity
 */
class TraceHeaderTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystone-v2/common", params)

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def "Tracing header should include in request to Identity"() {

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = "123456"
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/123456", method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Things are forward to the origin, because we're not validating existence of tenant"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }
    }

    @Unroll("Failed response from repose with Identity respcode #identityrespcode")
    def "Tracing header should include in Failed response from repose"() {

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = "123456"
        }

        fakeIdentityService.validateTokenHandler = {
            tokenId, request, xml ->
                return new Response(identityrespcode)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/123456", method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then:
        mc.receivedResponse.code == respcode
        mc.receivedResponse.headers.contains("x-trans-id")
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }

        where:
        identityrespcode | respcode
        "401"            | "500"
        "403"            | "500"
        "413"            | "503"
        "404"            | "401"
        "500"            | "500"
    }
}
