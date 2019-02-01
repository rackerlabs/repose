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
package features.filters.valkyrie

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static java.nio.charset.StandardCharsets.UTF_8
import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ROLES_MAP

/**
 * Created by jennyvo on 10/7/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class DeviceLevelPermissionToRolesTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static readOnlyMethod = ['GET', 'HEAD']
    def static readWriteMethod = ['PUT', 'POST', 'DELETE']
    def static readOnlyPerm = ['view_product']
    def static readWritePerm = ['edit_product', 'admin_product']

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/translatepermissionstoroles", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/translatepermissionstoroles/devicelevel", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
        fakeIdentityService.resetHandlers()
        fakeIdentityService.resetDefaultParameters()
        fakeValkyrie.resetHandlers()
        fakeValkyrie.resetParameters()
    }

    @Unroll("#method device #deviceID with permission #permission, tenant: #tenantID should return a #responseCode")
    def "Test verify only user request device permission will be added to x-roles"() {
        given: "A device ID with a particular permission level defined in Valkyrie"

        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        and: "Valkyrie user permissions"
        def permissions = [
            permission,
            "upgrade_account",
            "edit_ticket",
            "edit_domain",
            "manage_users",
            "view_domain",
            "view_reports"
        ]

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == deviceID

        when: "headers are parsed at the origin service"
        def roles = mc.handlings[0].request.headers.findAll(ROLES)
        def tenantToRolesHeader = mc.handlings[0].request.headers.getFirstValue(TENANT_ROLES_MAP)
        def tenantToRoles = new JsonSlurper().parseText(new String(Base64.decoder.decode(tenantToRolesHeader), UTF_8))

        and: "the roles are scoped to those associated with the requested tenant"
        def tenantScopedRoles = tenantToRoles[tenantID] as List

        then: "in-scope permissions are added to the request as roles"
        roles.containsAll(permissions)
        tenantScopedRoles.containsAll(permissions)

        and: "out-of-scope permissions are not added to the request"
        roles.intersect(notincluderoles).isEmpty()
        tenantScopedRoles.intersect(notincluderoles).isEmpty()

        where:
        method   | tenantID       | deviceID | permission      | notincluderoles                   | responseCode
        "GET"    | randomTenant() | "520707" | "view_product"  | ['admin_product', 'edit_product'] | "200"
        "HEAD"   | randomTenant() | "520707" | "view_product"  | ['admin_product', 'edit_product'] | "200"
        "GET"    | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "HEAD"   | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "PUT"    | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "POST"   | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "PATCH"  | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "DELETE" | randomTenant() | "520707" | "admin_product" | ['view_product', 'edit_product']  | "200"
        "GET"    | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
        "HEAD"   | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
        "PUT"    | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
        "POST"   | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
        "PATCH"  | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
        "DELETE" | randomTenant() | "520708" | "edit_product"  | ['view_product', 'admin_product'] | "200"
    }

    @Unroll
    def "#method with permissions #permissions should return an OK (200)"() {
        given: "A device ID with a particular permission level defined in Valkyrie"
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = randomTenant()
        }

        fakeValkyrie.with {
            device_perm = (permissions as List<String>).join(",")
            validationSuccess = { Map<String, String> params -> createValkyrieDevicePermissionsBody(params) }
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + fakeValkyrie.device_id, method: method,
            headers: [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityService.client_token,
            ]
        )

        then: "check response"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0].request.headers.getFirstValue("x-device-id") == fakeValkyrie.device_id

        when: "headers are parsed at the origin service"
        def roles = mc.handlings[0].request.headers.findAll(ROLES)
        def tenantToRolesHeader = mc.handlings[0].request.headers.getFirstValue(TENANT_ROLES_MAP)
        def tenantToRoles = new JsonSlurper().parseText(new String(Base64.decoder.decode(tenantToRolesHeader), UTF_8))

        and: "the roles are scoped to those associated with the requested tenant"
        def tenantScopedRoles = tenantToRoles[fakeIdentityService.client_tenantid as String] as List

        then: "in-scope permissions are added to the request as roles"
        roles.containsAll(readWritePerm)
        tenantScopedRoles.containsAll(readWritePerm)
        if (readOnlyMethod.contains(method)) {
            assert roles.containsAll(readOnlyPerm)
            assert tenantScopedRoles.containsAll(readOnlyPerm)
        }

        where:
        [permissions, method] << [
            (readOnlyPerm + readWritePerm).permutations(),
            (readOnlyMethod + readWriteMethod),
        ].combinations()
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }

    def createValkyrieDevicePermissionsBody(Map<String, String> permissions) {
        def deviceId = permissions['deviceID']
        def permissionNames = permissions['permission'].tokenize(',')
        def contactPermissions = permissionNames.inject("") { acc, permissionName ->
            """$acc${if (!acc.empty) "," else ""}
            |    {
            |        "account_number":862323,
            |        "contact_id": 818029,
            |        "id": 0,
            |        "item_id": $deviceId,
            |        "item_type_id" : 1,
            |        "item_type_name" : "devices",
            |        "permission_name" : "$permissionName",
            |        "permission_type_id" : 12
            |    }""".stripMargin()
        }
        return """{
        |  "contact_permissions" : [
        |    ${contactPermissions}
        |  ]
        |}""".stripMargin()
    }
}
