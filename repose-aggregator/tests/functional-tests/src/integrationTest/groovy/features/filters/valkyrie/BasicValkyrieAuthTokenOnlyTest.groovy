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
import scaffold.category.Filters
import spock.lang.Unroll

/**
  Created by joelrizner on 5/31/16
  Basically a copy of BasicValkyrieTest where the config contains no username or password headers.
 */
@Category(Filters)
class BasicValkyrieAuthTokenOnlyTest extends ReposeValveTest {
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
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/authtokenonly", params);

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

    @Unroll("authentication: device with #validAuth is passed auth #requestedAuth should return a #responseCode")
    def "Test validity of requested auth codes"() {
        given: "A device with a valid authentication token"
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = randomTenant()
        }

        fakeValkyrie.with {
            valid_auth = validAuth
            device_id = "520707"
            device_perm = "view_product"
        }

        when: "a request is made with a unique auth token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/520707", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': requestedAuth,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("x-trans-id")
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }


        where:
        validAuth       | requestedAuth       | responseCode
        "A21TH7"        | "A21TH7"            | "200"
        "BBBEYWW23BBB"  | "A21TH7"            | "403"
        "BBBBCC"        | "BBBBCC"            | "200"
        "ABBBTEST"      | "ABBBTEST"          | "200"
        "RESPONSEA"     | "RESPONSEB"         | "403"

    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
