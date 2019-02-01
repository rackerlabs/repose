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
package features.filters.keystonev2.admintoken

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

/**
 * Specific tests for admin token
 */
@Category(Filters)
class IdentityAdminTokenTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/removetenant/tenantednondelegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)
    }

    def setup() {
        sleep 500
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.resetCounts()
    }

    @Unroll("Sending request with admin response set to HTTP #adminResponseCode")
    def "when failing to authenticate admin client"() {

        given:
        fakeIdentityV2Service.with {
            client_tenantid = reqTenant
            admin_userid = "999999"
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)

        }

        if (adminResponseCode != 200) {
            fakeIdentityV2Service.generateTokenHandler = {
                request, xml ->
                    new Response(adminResponseCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        //mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        where:
        reqTenant | adminResponseCode | responseCode | responseBody                                     | orphanedHandlings
        1113      | 500               | "502"        | ""                                               | 1
        1112      | 404               | "500"        | fakeIdentityV2Service.identityFailureXmlTemplate | 1
        1111      | 200               | "200"        | fakeIdentityV2Service.identitySuccessXmlTemplate | 3
    }

    def "when a token fails for unauthorized admin token, then it should get a new admin and try again without hitting the Akka cache."() {
        given: "an Identity service that will fail with UNAUTHORIZED (401) on the first token validation attempt"
        fakeIdentityV2Service.with {
            client_tenantid = 'un-cached-tenant'
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def counter = 0
        fakeIdentityV2Service.validateTokenHandler = {
            tokenId, tenantid, request ->
                counter++
                switch (counter) {
                    case 1:
                        new Response(SC_UNAUTHORIZED)
                        break
                    case 2:
                        fakeIdentityV2Service.validateToken(tokenId, tenantid, request)
                        break
                    default:
                        new Response(SC_INTERNAL_SERVER_ERROR)
                }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/${fakeIdentityV2Service.client_tenantid}/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token
                ]
        )

        then: "the response should be OK (200)"
        fakeIdentityV2Service.generateTokenCount == 1
        fakeIdentityV2Service.validateTokenCount == 2
        fakeIdentityV2Service.getGroupsCount == 1
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        // The second admin token request will hit the Akka cache so it isn't 5.
        mc.orphanedHandlings.size() == 4
    }
}
