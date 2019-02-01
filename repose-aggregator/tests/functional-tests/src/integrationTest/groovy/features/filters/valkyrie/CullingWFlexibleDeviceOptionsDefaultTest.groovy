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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import static features.filters.valkyrie.CullingWFlexibleDeviceOptionsTestsHelper.jsonrespbody
import static features.filters.valkyrie.CullingWFlexibleDeviceOptionsTestsHelper.randomTenant

@Category(Filters)
class CullingWFlexibleDeviceOptionsDefaultTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    static MockIdentityV2Service fakeIdentityService
    static MockValkyrie fakeValkyrie
    static Map params = [:]

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params)
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/collectionresources", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    @Unroll
    def "Fail default - permission: #permission for #method with tenant: #tenantID and deviceIDs: #deviceID, #deviceID2 should return a #responseCode"() {
        given: "a user defined in Identity"
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        and: "a JSON Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method,
            headers: [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityService.client_token,
                'x-contact-id': '123456'
            ],
            defaultHandler: jsonResp
        )

        then: "check response"
        mc.receivedResponse.code == responseCode

        where:
        method | tenantID       | deviceID | deviceID2 | permission     | responseCode
        "GET"  | randomTenant() | "520707" | "511123"  | "view_product" | "500"
        "GET"  | randomTenant() | "520708" | "511123"  | "view_product" | "500"
        "GET"  | randomTenant() | "520707" | "520708"  | "view_product" | "500"
        "GET"  | randomTenant() | "520705" | "520706"  | "view_product" | "500"
    }
}
