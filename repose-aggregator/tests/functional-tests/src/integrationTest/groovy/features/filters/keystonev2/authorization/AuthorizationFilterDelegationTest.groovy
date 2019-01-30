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
package features.filters.keystonev2.authorization

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Filters)
class AuthorizationFilterDelegationTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/delegating", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(
            properties.identityPort,
            'identity service',
            null,
            fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.client_tenantid = UUID.randomUUID().toString()
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        fakeIdentityV2Service.originServicePort = properties.targetPort
        reposeLogSearch.cleanLog()
    }

    def "User without valid service endpoint Port should receive a delegated 200 OK response "() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: [
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                'X-Tenant-ID': fakeIdentityV2Service.client_tenantid])

        then: "User should receive a 200 OK response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("Delegating with status 403 caused by: User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Delegation header is present"
        def delegatedHeader = mc.handlings[0].request.headers.findAll("X-Delegated").toString()
        delegatedHeader.contains("status_code=403")
        delegatedHeader.contains("component=keystone-v2")
        delegatedHeader.contains("message=User did not have the required endpoint")
        delegatedHeader.contains("q=0.8")
    }

    def "User without valid service endpoint URL should receive a delegated 200 OK response "() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.endpointUrl = "invalidurl"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: [
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                'X-Tenant-ID': fakeIdentityV2Service.client_tenantid])

        then: "User should receive a 200 OK response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("Delegating with status 403 caused by: User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Delegation header is present"
        def delegatedHeader = mc.handlings[0].request.headers.findAll("X-Delegated").toString()
        delegatedHeader.contains("status_code=403")
        delegatedHeader.contains("component=keystone-v2")
        delegatedHeader.contains("message=User did not have the required endpoint")
        delegatedHeader.contains("q=0.8")
    }

    @Unroll
    def "User with incorrect region (#serviceRegion) on a configured endpoint should receive a delegated 200 OK response"() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.region = serviceRegion

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "User should receive a 200 OK response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("Delegating with status 403 caused by: User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Delegation header is present"
        def delegatedHeader = mc.handlings[0].request.headers.findAll("X-Delegated").toString()
        delegatedHeader.contains("status_code=403")
        delegatedHeader.contains("component=keystone-v2")
        delegatedHeader.contains("message=User did not have the required endpoint")
        delegatedHeader.contains("q=0.8")

        where:
        serviceRegion << ["DFW", "RegionOne", null]
    }
}
