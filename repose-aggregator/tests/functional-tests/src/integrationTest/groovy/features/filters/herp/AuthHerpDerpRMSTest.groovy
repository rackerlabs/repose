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

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/16/15.
 * Update on 01/21/16
 *  - Replace client-auth-n with keystone-v2 filter
 */
@Ignore
@Category(Filters)
class AuthHerpDerpRMSTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/wderpandrms", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/wderpandrms/wkeystonev2", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    @Unroll("Req with auth resp: #authRespCode")
    def "When req with invalid token using delegable mode with quality"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
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

        then:
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("Content-Type")
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0
        mc.getOrphanedHandlings().size() >= 1 // at least there one validate token call
        //mc.getOrphanedHandlings().size() == 2 //why do we care about this? look into it later

        /* expected internal delegated messages to derp from authn:
            Since auth delegating quality higher than validation filter we expect delegating message
            "status_code=401.component=client-auth-n.message=Unable to validate token:\\s.*;q=0.6"
            "status_code=500.component=client-auth-n.message=Failure in Auth-N filter.;q=0.6"
        */
        where:
        authRespCode | responseCode | msgBody
        404          | "401"        | "Resource not found for validate token request"
        401          | "500"        | "Admin token unauthorized to make validate token request"
    }
}

