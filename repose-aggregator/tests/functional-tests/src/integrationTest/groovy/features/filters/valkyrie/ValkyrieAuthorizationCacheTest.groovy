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

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

/**
 * Created by jennyvo on 4/21/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class ValkyrieAuthorizationCacheTest extends ReposeValveTest {
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

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def "Test Valkyrie Authorization Cache"() {
        given: "A device ID with a particular permission level defined in Valkyrie"
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = "520707"
            device_perm = "view_product"
        }

        when: "a request is made against a device with Valkyrie set permissions"
        fakeValkyrie.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520707", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        fakeValkyrie.getAuthorizationCount() == 1

        when: "send another request with same device, permission same client_token"
        fakeValkyrie.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520707", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        fakeValkyrie.getAuthorizationCount() == 0
    }

    def "Test Cache Timeout"() {
        given: "A device ID with a particular permission level defined in Valkyrie"
        DateTime initialCacheValidation = DateTime.now()
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = "520708"
            device_perm = "admin_product"
        }

        when: "sub-sequence request with same device, permission and client_token not exceeding the cache expiration"
        fakeValkyrie.resetCounts()
        DateTime minimumCacheExpiration = initialCacheValidation.plusMillis(3000)
        while (minimumCacheExpiration.isAfterNow()) {
            MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520708", method: "GET",
                    headers: [
                            'content-type': 'application/json',
                            'X-Auth-Token': fakeIdentityService.client_token,
                    ]
            )
            mc.receivedResponse.code.equals('200')
        }

        then: "should count only for 1st time then all sub-sequence calls should hit cache"
        fakeValkyrie.getAuthorizationCount() == 1

        when: "Cache is expire"
        fakeValkyrie.resetCounts()
        sleep(500)

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520708", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        then: "should re-authenticate"
        fakeValkyrie.getAuthorizationCount() == 1

    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
