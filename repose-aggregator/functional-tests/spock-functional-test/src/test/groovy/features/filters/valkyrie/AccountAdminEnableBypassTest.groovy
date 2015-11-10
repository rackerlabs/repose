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
import framework.mocks.MockIdentityService
import framework.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by mlopez on 11/10/15.
 * if account_admin role and enable-bypass-account-admin is true in config, don't bother making the inventory call and
 * just allow all the things.
 */
class AccountAdminEnableBypassTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/accountadmin", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/accountadmin/enablebypass", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
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

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll
    def "user with account_admin role can access a device it has permissions to for method #method"() {
        given:
        def deviceId = "520707"
        def tenantId = randomTenant()
        def permission = "account_admin"

        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantId
        }

        fakeValkyrie.with {
            device_id = deviceId + "0" // don't give user permission to device
            device_perm = "edit_product"
            account_perm = permission
            inventory_multiplier = 500
        }

        when: "a #method request is made to access device #deviceID"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceId, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        def accountid = fakeValkyrie.tenant_id
        def contactid = fakeIdentityService.contact_id

        then: "the response should be #responseCode and #permission should be in the Requests the X-Roles header"
        mc.receivedResponse.code == "200"
        // user device permission translate to roles
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains(permission)
        mc.getHandlings().get(0).getRequest().headers.getFirstValue("x-device-id") == deviceId
        // orphanedhandlings should include the original call + account inventoty call
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/permissions/contacts/any/by_contact/" + contactid + "/effective")
        !mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/inventory")

        where:
        method << ["HEAD", "GET", "PUT", "POST", "PATCH", "DELETE"]
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
