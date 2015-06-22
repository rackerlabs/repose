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
package features.filters.herp

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/16/15.
 */
class AuthHerpDerpRMSMultiMatchValidatorPrefTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatormultimatch", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatormultimatch/wvalidatorpreference", params)

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
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
    These tests are to verify the delegation of authn failures to the derp filter, which then forwards
    that information back to the client.  The origin service, thus, never gets invoked.
    */
    @Unroll("#token_id expireat: #expireat, respcode: #responseCode and #msgBody")
    def "When req with invalid token using delegable mode with quality"() {
        given:
        fakeIdentityService.with {
            client_token = token_id
            tokenExpiresAt = (new DateTime()).plusDays(1);
        }

        fakeIdentityService.validateTokenHandler = {
            tokenId, request, xml ->
                new Response(authRespCode)
        }

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1234",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then:
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("content-type")
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0
        mc.getOrphanedHandlings().size() == orphans

        /* expected internal delegated messages to derp from validator:
            Since auth delegating quality is hower than validation filter we expect delegating message
            "status_code=404.component=api-validator.message=Response not found:\\s.*;q=0.7"
        */
        where:
        authRespCode | responseCode | msgBody                                               | token_id            | expireat                       | orphans
        404          | "404"        | "Resource not found"                                  | UUID.randomUUID()   | (new DateTime()).plusDays(1)   | 2
        404          | "404"        | "Resource not found"                                  | ""                  | (new DateTime()).plusDays(1)   | 0
        404          | "404"        | "Resource not found"                                  | UUID.randomUUID()   | (new DateTime()).minusDays(1)  | 1
        404          | "404"        | "Resource not found"                                  | ""                  | (new DateTime()).minusDays(1)  | 0
    }
}

