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

/**
 * Created by jennyvo on 8/31/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
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

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }
    def "Get Account level permissions and translate to roles" () {
        given:
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }
        fakeValkyrie.with {
                    account_perm = "test_perm"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/account/permissions", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then:
        mc.receivedResponse.code == responseCode
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("test_perm")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("upgrade_account")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("edit_ticket")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("edit_domain")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("manage_users")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("view_domain")
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains("view_reports")

        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }

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
