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
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.HeaderCollection
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ID
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ROLES_MAP
import static org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES

@Category(Filters)
class TenantCullingFilterStandaloneFunctionalTest extends ReposeValveTest {
    static jsonSlurper = new JsonSlurper()

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        deproxy.addEndpoint(properties.targetPort)
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/tenantculling/noKeystone', params)
        repose.start()
    }

    @Unroll
    def "#testName returns server error"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR

        where:
        testName                           | headers
        'Unencoded tenant-to-roles map'    | [(TENANT_ROLES_MAP): '{"defaultTenant": [], "tenant1": ["role1", "role2"], "tenant3": ["role3"]}']
        'Invalid JSON tenant-to-roles map' | [(TENANT_ROLES_MAP): Base64.encoder.encodeToString('"defaultTenant": [], "tenant1": ["role1", "role2"], "tenant3": ["role3"]}'.bytes)]
        'Garbage tenant-to-roles map'      | [(TENANT_ROLES_MAP): 'u832hrh349tb31>{}_+{}']
        'No tenant-to-roles map'           | null
    }

    def "culls all tenants when no relevant roles exist"() {
        given: "a tenant-to-roles map"
        Map tenantToRolesMap = ["defaultTenant": [], "tenant1": ["role1", "role2"], "tenant3": ["role3"]]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            headers: [(TENANT_ROLES_MAP): encodeTenantToRolesMap(tenantToRolesMap)]
        )

        then: "the origin service should not receive any tenants"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0].request.headers.findAll(TENANT_ID).isEmpty()
        decodeTenantToRolesMap(mc.handlings[0].request.headers.findAll(TENANT_ROLES_MAP).first()).isEmpty()
    }

    def "sends multiple tenants matching multiple relevant roles"() {
        given: "a tenant-to-roles map"
        Map tenantToRolesMap = ["defaultTenant": [], "tenant1": ["role1", "role2"], "tenant3": ["role3"]]

        when: "the request is made"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            method: 'GET',
            headers: [
                (RELEVANT_ROLES)  : 'role1,role3',
                (TENANT_ROLES_MAP): encodeTenantToRolesMap(tenantToRolesMap)
            ]
        )

        then: "the origin service should not receive any tenants"
        mc.handlings.size() == 1
        mc.receivedResponse.code as Integer == SC_OK

        when: "the origin service request headers are parsed"
        HeaderCollection requestHeaders = mc.handlings[0].request.headers
        List originTenantIds = requestHeaders.findAll(TENANT_ID).collect { it.split(",") }.flatten()
        Map originTenantToRolesMap = decodeTenantToRolesMap(requestHeaders.findAll(TENANT_ROLES_MAP).first())

        then: "the origin service should have received relevant tenants and roles"
        originTenantIds == ['tenant1', 'tenant3']
        originTenantToRolesMap == (tenantToRolesMap - ['defaultTenant': []])
    }

    Map decodeTenantToRolesMap(String encodedTenantToRolesMap) {
        jsonSlurper.parse(Base64.decoder.decode(encodedTenantToRolesMap)) as Map<String, Set<String>>
    }

    String encodeTenantToRolesMap(Map tenantToRolesMap) {
        Base64.encoder.encodeToString(JsonOutput.toJson(tenantToRolesMap).bytes)
    }
}
