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
package features.filters.identityv3.delegating

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/18/14.
 */
@Category(Filters)
class IdentityV3AuthZDelegatingTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    //def static targetPort

    static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/delegating/authz", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
        //targetPort = properties.targetPort
    }

    @Unroll("#method request to #endpointResponse")
    def "When user is authorized should forward request with failure msg to origin service"() {
        given:
        fakeIdentityV3Service.with {
            endpointUrl = endpointResponse
        }
        def token = "some-random-token"
        def delegatingMsg = "status_code=403.component=openstack-identity-v3.message=Invalid endpoints for token:\\s.*;q=0.5"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.reposePort}/v3/${fakeIdentityV3Service.client_token}/ss", method: method, headers: ['X-Subject-Token': token])

        then: "User should receive a #statusCode response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        [endpointResponse, method] << [["myhost.com", "test.com"], ["GET", "POST", "PUT", "PATCH", "DELETE"]].combinations()
    }

    def "When request send to invalid auth service port delegating will forward the failure to origin service"() {

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityV3Service.with {
            client_token = token
            servicePort = 99999
        }
        def delegatingMsg = "status_code=403.component=openstack-identity-v3.message=Invalid endpoints for token:\\s.*;q=0.5"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.reposePort}/v3/${token}/ss", method: 'GET', headers: ['X-Subject-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-v3.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        //foundLogs.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg
    }

    @Unroll("Identity broken admin call with: #errorCode")
    def "When req failed with auth admin service broken with delegating mode req is forwarded to service endpoint with failure message"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()

        }
        fakeIdentityV3Service.generateTokenHandler = { request -> return new Response(errorCode) }
        def delegatingMsg = "status_code=403.component=openstack-identity-v3.message=.*;q=0.5"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        errorCode << ["400", "401", "403", "404", "500", "503"]
    }

    @Unroll("Identity broken endpoint with: #errorCode")
    def "When req failed with auth service endpoint broken with delegating mode req is forwarded to service endpoint with failure message"() {
        given:
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()

        }
        fakeIdentityV3Service.getCatalogHandler = { tokenId, request -> return new Response(errorCode) }
        def delegatingMsg = "status_code=403.component=openstack-identity-v3.message=.*;q=0.5"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        errorCode << ["400", "401", "403", "404", "500", "503"]
    }
}
