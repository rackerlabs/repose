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
import scaffold.category.Slow
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN


@Category(Filters)
class AuthorizationFilterTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

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

    def "User without valid service endpoint Port should receive a 403 FORBIDDEN response "() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: [
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                'X-Tenant-ID': fakeIdentityV2Service.client_tenantid])

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have never made it to the origin service"
        mc.handlings.size() == 0
    }

    def "User without valid service endpoint URL should receive a 403 FORBIDDEN response "() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.endpointUrl = "invalidurl"

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: [
                'X-Auth-Token': fakeIdentityV2Service.client_token,
                'X-Tenant-ID': fakeIdentityV2Service.client_tenantid])

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have never made it to the origin service"
        mc.handlings.size() == 0
    }

    @Unroll
    def "User sends the prefixed tenant (#tenantPrefix) in the URI should succeed and be extracted"() {
        given: "User has a prefixed tenant ID"
        String tenantId = fakeIdentityV2Service.client_tenantid
        fakeIdentityV2Service.client_tenantid = tenantPrefix + tenantId

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/extract/$tenantId/ss",
            method: 'GET',
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Matching user tenant ID should be placed in a header"
        mc.handlings[0].request.headers.findAll("x-tenant-id").toString().contains(fakeIdentityV2Service.client_tenantid + ";q=0.6")

        where:
        tenantPrefix << ["foo:", "bar-", ""]
    }

    def "User with the endpoint configured in their endpoints list with tenant appended"() {
        given: "IdentityService is configured with tenant appended"
        fakeIdentityV2Service.appendedflag = true

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v1/appended/" + fakeIdentityV2Service.client_tenantid + "/ss",
            method: 'GET',
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        and: "Tenant should be extracted from header"
        mc.handlings[0].request.headers.findAll("x-tenant-id").toString().contains(fakeIdentityV2Service.client_tenantid + ";q=0.6")
    }

    @Unroll
    def "User with incorrect region (#serviceRegion) on a configured endpoint should receive a 403 FORBIDDEN response"() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.region = serviceRegion

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "User should receive a 403 FORBIDDEN response"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN

        and: "The reason should have been logged"
        def foundLogs = reposeLogSearch.searchByString("User did not have the required endpoint")
        foundLogs.size() == 1

        and: "It should have never made it to the origin service"
        mc.handlings.size() == 0

        where:
        serviceRegion << ["DFW", "RegionOne", null]
    }

    @Unroll
    def "Tenanted with pre-authorize role #role"() {
        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        fakeIdentityV2Service.service_admin_role = role

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/v2/ss",
            method: 'GET',
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should return all roles"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have made it to the origin service"
        mc.handlings.size() == 1

        where:
        role << ["serviceAdmin", "racker"]
    }
}
