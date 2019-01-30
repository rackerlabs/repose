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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.lang3.RandomStringUtils
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static org.openrepose.commons.utils.http.CommonHttpHeader.AUTH_TOKEN
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ROLES_MAP
import static org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES

@Category(Filters)
class TenantCullingFilterWithAuthenticationFunctionalTest extends ReposeValveTest {
    static final def BODY_CHARS = '0123456789ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz '

    static jsonSlurper = new JsonSlurper()

    @Shared
    MockIdentityV2Service fakeIdentityService
    @Shared
    def tenantIdDef = 'tenant-id-default'
    @Shared
    def tenantIdOne = 'tenant-id-one'
    @Shared
    def tenantIdTwo = 'tenant-id-two'
    @Shared
    def roleNameOne = 'role-name-one'
    @Shared
    def tenantedRoleNameOne = "$roleNameOne/$tenantIdOne"
    @Shared
    def roleNameTwo = 'role-name-two'
    @Shared
    def tenantedRoleNameTwo = "$roleNameTwo/$tenantIdTwo"
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

    def "does not send default Tenant"() {
        given: "a configured Keystone/Identity and appropriate request header"
        fakeIdentityService.client_tenantid = UUID.randomUUID().toString()
        def headers = [
            (AUTH_TOKEN): fakeIdentityService.client_token
        ]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "the origin service should not receive the default tenant"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0].request.headers.findAll(TENANT_ID).isEmpty()
        decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first()).isEmpty()
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

        then: "the origin service should respond to the request"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK

        and: "the tenant header should contain the appropriate tenant(s)"
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID).collect { it.split(",") }.flatten()
        tenantIds.size() == expected.size()
        tenantIds.containsAll(expected)
        tenantIds.disjoint(notExpected)

        when: "the tenant-to-roles map is decoded from the header"
        Map tenantToRolesMap = decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first())
        Map expectedTenantToRolesMap = roles['roles'].groupBy { it.tenantId }.collectEntries {
            [(it.key): it.value.collect { it.name }]
        }.findAll { expected.contains(it.key) }

        then: "the tenant-to-roles map header should contain the appropriate entries"
        tenantToRolesMap == expectedTenantToRolesMap

        where:
        testName                                                        | clientTenantId | roles              | relevant                                    | expected                   | notExpected
        "Sends Tenant that matches a role"                              | null           | [roles: oneRole]   | roleNameOne                                 | [tenantIdOne]              | [tenantIdDef]
        "Sends Tenant that matches a role with default tenant"          | tenantIdDef    | [roles: oneRole]   | roleNameOne                                 | [tenantIdOne]              | [tenantIdDef]
        "Sends no Tenant with role mismatch"                            | tenantIdDef    | [roles: oneRole]   | roleNameTwo                                 | []                         | [tenantIdDef, tenantIdOne]
        "Sends Tenant that matches single role"                         | null           | [roles: twoRoles]  | roleNameOne                                 | [tenantIdOne]              | [tenantIdDef, tenantIdTwo]
        "Sends multiple Tenants that match single role"                 | null           | [roles: sameRoles] | roleNameOne                                 | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
        "Sends multiple Tenants that match multiple roles"              | null           | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo"                 | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
        "Sends Tenants with and without roles"                          | tenantIdDef    | [roles: twoRoles]  | roleNameOne                                 | [tenantIdOne]              | [tenantIdDef, tenantIdTwo]
        "Sends multiple Tenants that match single role"                 | tenantIdDef    | [roles: sameRoles] | roleNameOne                                 | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
        "Sends multiple Tenants that match multiple roles"              | tenantIdDef    | [roles: twoRoles]  | "$roleNameOne,$roleNameTwo"                 | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
        "Sends Tenant that matches a tenanted role"                     | null           | [roles: twoRoles]  | tenantedRoleNameOne                         | [tenantIdOne]              | [tenantIdDef, tenantIdTwo]
        "Sends Tenant that matches a tenanted role with default tenant" | tenantIdDef    | [roles: twoRoles]  | tenantedRoleNameOne                         | [tenantIdOne]              | [tenantIdDef, tenantIdTwo]
        "Sends multiples Tenants that match a tenanted role"            | null           | [roles: sameRoles] | tenantedRoleNameOne                         | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
        "Sends multiples Tenants that match multiple tenanted roles"    | null           | [roles: twoRoles]  | "$tenantedRoleNameOne,$tenantedRoleNameTwo" | [tenantIdOne, tenantIdTwo] | [tenantIdDef]
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

        then: "the origin service should not receive any tenants"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        tenantIds.isEmpty()
        !tenantIds.contains(tenantIdOne)
        decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first()).isEmpty()
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
        tenantIds.isEmpty()
        decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first()).isEmpty()

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

        and: "the tenant-to-roles map is decoded from the header"
        Map tenantToRolesMap = decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first())

        then: "the tenant-to-roles map header should contain the appropriate entries"
        tenantToRolesMap == [(tenantIdOne): [roleNameOne]]

        and: "the origin service should receive the appropriate tenant(s)"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        def tenantIds = mc.handlings[0].request.headers.findAll(TENANT_ID)
        def expected = [tenantIdOne]
        def notExpected = [tenantIdDef, tenantIdTwo]
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

    Map decodeTenantToRolesMap(String encodedTenantToRolesMap) {
        jsonSlurper.parse(Base64.decoder.decode(encodedTenantToRolesMap)) as Map
    }

    String encodeTenantToRolesMap(Map tenantToRolesMap) {
        Base64.encoder.encodeToString(JsonOutput.toJson(tenantToRolesMap).bytes)
    }
}
