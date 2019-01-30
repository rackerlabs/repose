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
package features.filters.urlextractortoheader

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Created by jennyvo on 11/23/15.
 *  Test valkyrie without need of validator but using url-extractor-to-header
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class UrlExtractorToHeaderWValkyrieTest extends ReposeValveTest {
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
        repose.configurationProvider.applyConfigs("features/filters/urlextractortoheader", params);
        repose.configurationProvider.applyConfigs("features/filters/urlextractortoheader/wvalkyrie", params);

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

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceID: #deviceID should return a #responseCode")
    def "Test Valkyrie with url-extractor-to-header"() {
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
        if (responseCode.equals("200")) {
            assertTrue(mc.handlings.get(0).request.headers.contains("x-device-id"))
            assertThat(mc.handlings.get(0).request.headers.getFirstValue("x-device-id"), equalTo(deviceID))
        }
        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assertTrue(e.request.headers.contains("x-trans-id"))
        }

        where:
        method   | tenantID                   | deviceID | permission      | responseCode
        "GET"    | randomTenant()             | "520707" | "view_product"  | "200"
        "HEAD"   | randomTenant()             | "520707" | "view_product"  | "200"
        "GET"    | randomTenant() - "hybrid:" | "520707" | "view_product"  | "403"
        "PUT"    | randomTenant()             | "520707" | "view_product"  | "403"
        "POST"   | randomTenant()             | "520707" | "view_product"  | "403"
        "DELETE" | randomTenant()             | "520707" | "view_product"  | "403"
        "PATCH"  | randomTenant()             | "520707" | "view_product"  | "403"
        "GET"    | randomTenant()             | "520707" | "admin_product" | "200"
        "HEAD"   | randomTenant()             | "520707" | "admin_product" | "200"
        "PUT"    | randomTenant()             | "520707" | "admin_product" | "200"
        "POST"   | randomTenant()             | "520707" | "admin_product" | "200"
        "PATCH"  | randomTenant()             | "520707" | "admin_product" | "200"
        "DELETE" | randomTenant()             | "520707" | "admin_product" | "200"
        "GET"    | randomTenant()             | "520707" | "edit_product"  | "200"
        "HEAD"   | randomTenant()             | "520707" | "edit_product"  | "200"
        "PUT"    | randomTenant()             | "520707" | "edit_product"  | "200"
        "POST"   | randomTenant()             | "520707" | "edit_product"  | "200"
        "PATCH"  | randomTenant()             | "520707" | "edit_product"  | "200"
        "DELETE" | randomTenant()             | "520707" | "edit_product"  | "200"
        "GET"    | randomTenant()             | "520707" | ""              | "403"
        "HEAD"   | randomTenant()             | "520707" | ""              | "403"
        "PUT"    | randomTenant()             | "520707" | ""              | "403"
        "POST"   | randomTenant()             | "520707" | ""              | "403"
        "PATCH"  | randomTenant()             | "520707" | ""              | "403"
        "DELETE" | randomTenant()             | "520707" | ""              | "403"
        "GET"    | randomTenant()             | "520707" | "shazbot_prod"  | "403"
        "HEAD"   | randomTenant()             | "520707" | "prombol"       | "403"
        "PUT"    | randomTenant()             | "520707" | "hezmol"        | "403"
        "POST"   | randomTenant()             | "520707" | "_22_reimer"    | "403"
        "PATCH"  | randomTenant()             | "520707" | "blah"          | "403"
        "DELETE" | randomTenant()             | "520707" | "blah"          | "403"

    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
