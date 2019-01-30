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
 * Created by jennyvo on 9/11/15.
 *  Bypass the calls to Valkyrie if configured roles exist.
 *    This also means we will not do culling, device authorization or role translation
 */
@Category(Filters)
class GetDeviceBypassValkyrieAuthorizationTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/bypassvalkyrie", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
        fakeIdentityV2Service.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup () {
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.resetDefaultParameters()
    }

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceID: #deviceID should return a #responseCode")
    def "Test fine grain access of resources based on Valkyrie permissions (no rbac)"() {
        given: "A device ID with a particular permission level defined in Valkyrie"

        fakeIdentityV2Service.with {
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
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }
        // include request make to valkyrie
        assert mc.orphanedHandlings[3].request.path =~ "/account/(|-)\\d*/permissions/contacts/any/by_contact/(|-)\\d*/effective"


        where:
        method | tenantID       | deviceID | permission     | responseCode
        "GET"  | randomTenant() | "520707" | "view_product" | "200"
        "HEAD" | randomTenant() | "520707" | "view_product" | "200"
    }

    @Unroll
    def "Bypass valkyrie test"() {
        given: "token without tenantid associated with"
        fakeIdentityV2Service.with {
            client_token = "rackerSSO"
            service_admin_role = "racker"
            client_userid = "rackerSSOUsername"
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                ]
        )

        then:
        mc.receivedResponse.code == "200"
        // verify not interact with valkyrie
        if (mc.orphanedHandlings.size() > 0) {
            mc.orphanedHandlings.each {
                e -> assert !e.request.path.contains("/account")
            }
        }

        where:
        method   | deviceID | permission
        "GET"    | "520707" | "view_product"
        "HEAD"   | "520707" | "view_product"
        "GET"    | "520707" | "view_product"
        "PUT"    | "520707" | "view_product"
        "POST"   | "520707" | "view_product"
        "DELETE" | "520707" | "view_product"
        "PATCH"  | "520707" | "view_product"
        "GET"    | "520707" | "admin_product"
        "HEAD"   | "520707" | "admin_product"
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
