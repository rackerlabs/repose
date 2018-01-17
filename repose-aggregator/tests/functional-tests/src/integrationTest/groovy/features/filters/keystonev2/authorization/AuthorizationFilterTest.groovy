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
package features.filters.keystonev2.authorizationonly.serviceresponse

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Slow
import spock.lang.Unroll

@Category(Slow.class)
class AuthorizationFilterTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static token

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/nondelegating", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
            'identity service', null, fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.appendedflag = true
        token = UUID.randomUUID().toString()
        fakeIdentityV2Service.client_token = token
        fakeIdentityV2Service.originServicePort = properties.targetPort
        reposeLogSearch.cleanLog()
    }

    def "When user is not authorized should receive a 403 FORBIDDEN response"() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code == "403"
        foundLogs.size() == 1
        mc.handlings.size() == 0
    }

    def "User's invalid service endpoint should receive a 403 FORBIDDEN response "() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.endpointUrl = "invalidurl"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code == "403"
        foundLogs.size() == 1
        mc.handlings.size() == 0
    }

    def "Allow User's the endpoint configured in their endpoints list with tenant appended"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "Not Allow User's the endpoint configured in their endpoints list without tenant appended"() {
        given: "IdentityService is configured without tenant appended"
        fakeIdentityV2Service.appendedflag = false

        when: "User sends a request through repose without append"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': token])

        then: "User should receive a 403 response"
        mc.receivedResponse.code == "403"
        mc.handlings.size() == 0
    }

    @Unroll("User does not have right region: #serviceRegion")
    def "When user service endpoint doesn't have right region should receive a 403 FORBIDDEN response"() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.region = serviceRegion

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code == "403"
        foundLogs.size() == 1
        mc.handlings.size() == 0

        where:
        serviceRegion << ["DFW", "RegionOne", null]
    }
}
