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
 * Created by jennyvo on 4/21/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
class Masked404RespTest extends ReposeValveTest {
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
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/masked404resp", params);
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceID: #deviceID should return a #responseCode")
    def "Test fine grain access of resources based on Valkyrie permissions (no rbac)"() {
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

        where:
        method   | tenantID                   | deviceID | permission      | responseCode
        "GET"    | randomTenant()             | "520707" | "view_product"  | "200"
        "HEAD"   | randomTenant()             | "520707" | "view_product"  | "200"
        "GET"    | randomTenant() - "hybrid:" | "520707" | "view_product"  | "404"
        "PUT"    | randomTenant()             | "520707" | "view_product"  | "404"
        "POST"   | randomTenant()             | "520707" | "view_product"  | "404"
        "DELETE" | randomTenant()             | "520707" | "view_product"  | "404"
        "PATCH"  | randomTenant()             | "520707" | "view_product"  | "404"
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
        "GET"    | randomTenant()             | "520707" | ""              | "404"
        "HEAD"   | randomTenant()             | "520707" | ""              | "404"
        "PUT"    | randomTenant()             | "520707" | ""              | "404"
        "POST"   | randomTenant()             | "520707" | ""              | "404"
        "PATCH"  | randomTenant()             | "520707" | ""              | "404"
        "DELETE" | randomTenant()             | "520707" | ""              | "404"
        "GET"    | randomTenant()             | "520707" | "shazbot_prod"  | "404"
        "HEAD"   | randomTenant()             | "520707" | "prombol"       | "404"
        "PUT"    | randomTenant()             | "520707" | "hezmol"        | "404"
        "POST"   | randomTenant()             | "520707" | "_22_reimer"    | "404"
        "PATCH"  | randomTenant()             | "520707" | "blah"          | "404"
        "DELETE" | randomTenant()             | "520707" | "blah"          | "404"

    }

    @Unroll("tenant missing prefix 'hybrid': #tenantID, permission: #permission for #method and deviceID: #deviceID should return a #responseCode")
    def "Repose return 403 if tenant coming from identity prefix 'hybrid' is missing"() {
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

        where:
        method   | tenantID                        | deviceID | permission      | responseCode
        "GET"    | random.nextInt()                | "520707" | "view_product"  | "404"
        "HEAD"   | random.nextInt()                | "520707" | "view_product"  | "404"
        "GET"    | random.nextInt()                | "520707" | "admin_product" | "404"
        "HEAD"   | random.nextInt()                | "520707" | "admin_product" | "404"
        "PUT"    | random.nextInt()                | "520707" | "admin_product" | "404"
        "POST"   | random.nextInt()                | "520707" | "admin_product" | "404"
        "PATCH"  | random.nextInt()                | "520707" | "admin_product" | "404"
        "DELETE" | random.nextInt()                | "520707" | "admin_product" | "404"
        "GET"    | random.nextInt()                | "520707" | "edit_product"  | "404"
        "HEAD"   | random.nextInt()                | "520707" | "edit_product"  | "404"
        "PUT"    | random.nextInt()                | "520707" | "edit_product"  | "404"
        "POST"   | random.nextInt()                | "520707" | "edit_product"  | "404"
        "PATCH"  | random.nextInt()                | "520707" | "edit_product"  | "404"
        "DELETE" | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "GET"    | "dedicated:" + random.nextInt() | "520707" | "view_product"  | "404"
        "HEAD"   | "dedicated:" + random.nextInt() | "520707" | "view_product"  | "404"
        "GET"    | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "HEAD"   | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "PUT"    | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "POST"   | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "PATCH"  | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "DELETE" | "dedicated:" + random.nextInt() | "520707" | "admin_product" | "404"
        "GET"    | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "HEAD"   | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "PUT"    | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "POST"   | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "PATCH"  | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
        "DELETE" | "dedicated:" + random.nextInt() | "520707" | "edit_product"  | "404"
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
