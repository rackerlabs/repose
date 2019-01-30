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

import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * Created by tyler on 4/13/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Filters)
class DelegatingValkyrieTest extends ReposeValveTest {
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
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/delegable", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceID: #deviceID should contain a delegated message: #delegatedMsg")
    def "Test valkyrie filter delegable mode."() {
        given: "a configuration change where valkyrie filter delegates error messaging"

        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
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

        then: "origin service should be forwarded errors from valkyrie filter in header"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains(delegatedMsg)
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains("q=0.7")

        where:
        method   | tenantID                   | deviceID | permission     | delegatedMsg
        "GET"    | randomTenant() - "hybrid:" | "520707" | "view_product" | "status_code=403"
        "PUT"    | randomTenant()             | "520707" | "view_product" | "status_code=403"
        "POST"   | randomTenant()             | "520707" | "view_product" | "status_code=403"
        "DELETE" | randomTenant()             | "520707" | "view_product" | "status_code=403"
        "PATCH"  | randomTenant()             | "520707" | "view_product" | "status_code=403"
        "GET"    | randomTenant()             | "520707" | ""             | "status_code=403"
        "HEAD"   | randomTenant()             | "520707" | ""             | "status_code=403"
        "PUT"    | randomTenant()             | "520707" | ""             | "status_code=403"
        "POST"   | randomTenant()             | "520707" | ""             | "status_code=403"
        "DELETE" | randomTenant()             | "520707" | ""             | "status_code=403"
        "GET"    | randomTenant()             | "520707" | "shazbot_prod" | "status_code=403"
        "HEAD"   | randomTenant()             | "520707" | "prombol"      | "status_code=403"
        "PUT"    | randomTenant()             | "520707" | "hezmol"       | "status_code=403"
        "POST"   | randomTenant()             | "520707" | "_22_reimer"   | "status_code=403"
        "DELETE" | randomTenant()             | "520707" | "blah"         | "status_code=403"
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
