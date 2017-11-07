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
package features.filters.tenantculling

import org.apache.commons.lang3.RandomStringUtils
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID
import static org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES

class TenantCullingFilterFunctionalTest extends ReposeValveTest {
    static final def BODY_CHARS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz '

    @Shared
    MockIdentityV2Service fakeIdentityService
    @Shared
    def roleNameOne = 'role-name-one'
    @Shared
    def roleNameTwo = 'role-name-two'
    @Shared
    def tenantIdDef = 'tenant-id-default'
    @Shared
    def tenantIdOne = 'tenant-id-one'
    @Shared
    def tenantIdTwo = 'tenant-id-two'
    @Shared
    def oneRole = [[name: roleNameOne, tenantId: tenantIdOne]]
    @Shared
    def twoRoles = oneRole + [[name: roleNameTwo, tenantId: tenantIdTwo]]
    @Shared
    def sameRoles = oneRole + [[name: roleNameOne, tenantId: tenantIdTwo]]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort as Integer, 'origin service')
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/tenantculling/withKeystone', params)
        repose.start()

        fakeIdentityService = new MockIdentityV2Service(properties.identityPort as Integer, properties.targetPort as Integer)
        fakeIdentityService.checkTokenValid = true
        deproxy.addEndpoint(properties.identityPort as Integer, 'identity service', null, fakeIdentityService.handler)
    }

    def setup() {
        // This is required to ensure that one piece of the authentication data is changed
        // so that the cached version in the Akka Client is not used.
        fakeIdentityService.client_token = UUID.randomUUID().toString()
    }

    def "Sends default Tenant"() {
        given: "a configured Keystone/Identity and appropriate request header"
        fakeIdentityService.client_tenantid = UUID.randomUUID().toString()
        def headers = [
            (AUTH_TOKEN): fakeIdentityService.client_token
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive the default tenant"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 1
        tenantIds.contains(fakeIdentityService.client_tenantid)
    }

    @Unroll
    def "#testName"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.client_tenantid = clientTenantId
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(roles)
        def headers = [
            (AUTH_TOKEN)    : fakeIdentityService.client_token,
            (RELEVANT_ROLES): relevant
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive the appropriate tenant(s)"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == expected.size()
        tenantIds.containsAll(expected)
        tenantIds.disjoint(notExpected)

        where:
        testName                                                       | clientTenantId | roles              | relevant                    | expected                                | notExpected
        "Sends Tenant that matches a role"                             | null           | [roles: oneRole]   | roleNameOne                 | [tenantIdOne]                           | []
        "Sends Default and Tenant that matches a role"                 | tenantIdDef    | [roles: oneRole]   | roleNameOne                 | [tenantIdDef, tenantIdOne]              | []
        "Sends only Default with role mismatch"                        | tenantIdDef    | [roles: oneRole]   | roleNameTwo                 | [tenantIdDef]                           | [tenantIdOne]
        "Sends Tenant that matches single role"                        | null           | [roles: twoRoles]  | roleNameOne                 | [tenantIdOne]                           | [tenantIdTwo]
        "Sends multiple Tenants that match single role"                | null           | [roles: sameRoles] | roleNameOne                 | [tenantIdOne, tenantIdTwo]              | []
        "Sends multiple Tenants that match multiple roles"             | null           | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo" | [tenantIdOne, tenantIdTwo]              | []
        "Sends Tenants with and without roles"                         | tenantIdDef    | [roles: twoRoles]  | roleNameOne                 | [tenantIdDef, tenantIdOne]              | [tenantIdTwo]
        "Sends Default and multiple Tenants that match single role"    | tenantIdDef    | [roles: sameRoles] | roleNameOne                 | [tenantIdDef, tenantIdOne, tenantIdTwo] | []
        "Sends Default and multiple Tenants that match multiple roles" | tenantIdDef    | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo" | [tenantIdDef, tenantIdOne, tenantIdTwo] | []
    }

    def "Replaces Tenant"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.client_tenantid = UUID.randomUUID().toString()
        def headers = [
            (AUTH_TOKEN): fakeIdentityService.client_token,
            (TENANT_ID) : tenantIdOne
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should receive only the default tenant"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 1
        tenantIds.contains(fakeIdentityService.client_tenantid)
        !tenantIds.contains(tenantIdOne)
    }

    @Unroll
    def "Removes Tenant when #testName"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.client_tenantid = null
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler(roles)
        def headers = [
            (AUTH_TOKEN)    : fakeIdentityService.client_token,
            (TENANT_ID)     : tenantIdOne,
            (RELEVANT_ROLES): 'bogus'
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should not receive any tenants"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.size() == 0

        where:
        testName                                    | roles
        "no default and no tenanted roles"          | [:]
        "no default and no matching tenanted roles" | [roles: twoRoles]
    }

    @Unroll
    def "Culls Tenants and does not alter request and response bodies when HTTP verb is #httpMethod"() {
        given: "a configured Keystone/Identity and appropriate request headers"
        fakeIdentityService.client_tenantid = tenantIdDef
        fakeIdentityService.validateTokenHandler = createValidateTokenHandler([roles: twoRoles])
        def headers = [
            (AUTH_TOKEN)    : fakeIdentityService.client_token,
            (RELEVANT_ROLES): roleNameOne
        ]
        def requestBody = RandomStringUtils.random(256, BODY_CHARS)
        def responseBody = RandomStringUtils.random(256, BODY_CHARS)

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            method: httpMethod,
            headers: headers,
            requestBody: requestBody,
            defaultHandler: { new Response(SC_OK, null, null, responseBody) }
        )

        then: "the origin service should receive the appropriate tenant(s)"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        def expected = [tenantIdDef, tenantIdOne]
        def notExpected = [tenantIdTwo]
        tenantIds.size() == expected.size()
        tenantIds.containsAll(expected)
        tenantIds.disjoint(notExpected)

        and: "the request and response bodies should not be altered"
        mc.receivedResponse.body == responseBody
        mc.handlings[0].request.body == requestBody

        where:
        httpMethod << ['POST', 'PUT', 'PATCH', 'DELETE']
    }

    Closure<Response> createValidateTokenHandler(Map params) {
        { String tokenId, String tenantId, Request request ->
            def headers = [(CONTENT_TYPE): APPLICATION_JSON]
            def body = fakeIdentityService.createAccessJsonWithValues(params)
            new Response(SC_OK, null, headers, body)
        }
    }
}
