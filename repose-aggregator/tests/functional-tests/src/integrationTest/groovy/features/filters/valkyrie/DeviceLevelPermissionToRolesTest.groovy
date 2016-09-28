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

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import framework.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 10/7/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
class DeviceLevelPermissionToRolesTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

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

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        // account permissions are added to x-roles
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("upgrade_account")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("edit_ticket")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("edit_domain")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("manage_users")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("view_domain")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("view_reports")
        // user device permission translate to roles
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains(permission)
        mc.getHandlings().get(0).getRequest().headers.getFirstValue("x-device-id") == deviceID
        // other device permissions not included
        !mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains(notincluderoles[0])
        !mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains(notincluderoles[1])

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

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
