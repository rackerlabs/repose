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
import scaffold.category.Identity

@Category(Identity)
class ServiceListFeatureTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        cleanLogDirectory()

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorizationonly/servicelist", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)
    }

    def "user requests a URL that is in the user's service list"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }


    def "D-14988: client auth config should work without service-role element"() {
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + fakeIdentityV2Service.client_tenantid + "/ss", method: 'GET', headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "No NullPointerException is logged"
        List<String> logs = reposeLogSearch.searchByString("NullPointerException")
        logs.size() == 0

        and: "User should receive a 200 response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def "When user requests a URL service port is not in the user's service list should receive a 403 FORBIDDEN response"() {

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityV2Service.client_token = token
        fakeIdentityV2Service.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + token + "/ss", method: 'GET', headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }

    def "When user requests a URL that is not in the user's service list should receive a 403 FORBIDDEN response"() {

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        reposeLogSearch.cleanLog()

        def token = UUID.randomUUID().toString()
        fakeIdentityV2Service.client_token = token
        fakeIdentityV2Service.originServicePort = properties.targetPort
        fakeIdentityV2Service.endpointUrl = "invalidUrl"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + token + "/ss", method: 'GET', headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }
}
