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

import static java.nio.charset.StandardCharsets.UTF_8
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.TENANT_ROLES_MAP

/**
 * Created by jennyvo on 8/31/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class TranslatePermissionsToRolesTest extends ReposeValveTest {
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

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def "Get Account level permissions and translate to roles"() {
        given:
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }
        fakeValkyrie.with {
            account_perm = "test_perm"
        }

        and: "Valkyrie user permissions"
        def permissions = [
            "test_perm",
            "upgrade_account",
            "edit_ticket",
            "edit_domain",
            "manage_users",
            "view_domain",
            "view_reports"
        ]

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/account/permissions", method: method,
            headers: [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityService.client_token,
            ]
        )

        then: "the response has the expected response code"
        mc.receivedResponse.code == responseCode

        and: "the tracing header is sent on all requests"
        mc.receivedResponse.headers.contains("x-trans-id")
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }

        when: "headers are parsed at the origin service"
        def roles = mc.handlings[0].request.headers.findAll(ROLES)
        def tenantToRolesHeader = mc.handlings[0].request.headers.getFirstValue(TENANT_ROLES_MAP)
        def tenantToRoles = new JsonSlurper().parseText(new String(Base64.decoder.decode(tenantToRolesHeader), UTF_8))

        and: "the roles are scoped to those associated with the requested tenant"
        def tenantScopedRoles = tenantToRoles[tenantID] as List

        then: "permissions are added to the request as roles"
        roles.containsAll(permissions)
        tenantScopedRoles.containsAll(permissions)

        where:
        method | tenantID       | responseCode
        "GET"  | randomTenant() | "200"
    }

    def "Missing tenant id" () {
        given:
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = ""
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/account/permissions", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then:
        mc.receivedResponse.code == "401"
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
