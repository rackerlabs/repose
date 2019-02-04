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
package features.filters.keystonev2.multitenantsheaders

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Identity
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/26/14.
 * This test verify when user token having multi-tenant client-auth filter will retrieve
 * all tenants and put in multi x-tenant-id in headers
 */
@Category(Identity)
class MultiTenantHeadersTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/multitenantheader", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)


    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    @Unroll("#defaultTenant, #secondTenant, #requestTenant ")
    def "When user token have multi-tenant will retrieve all tenants in the header"() {
        given:
        fakeIdentityV2Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_tenantid = defaultTenant
            client_tenantid2 = secondTenant
            service_admin_role = "not-admin"
        }

        when:
        "User passes a request through repose with $requestTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").get(0).split(",").size() == numberTenants
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").get(0).contains(defaultTenant)
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").get(0).contains(secondTenant)
        }

        where:
        defaultTenant | secondTenant | requestTenant | clientToken       | serviceRespCode | numberTenants
        "123456"      | "nast-id"    | "123456"      | UUID.randomUUID() | "200"           | 2
        "123456"      | "nast-id"    | "nast-id"     | UUID.randomUUID() | "200"           | 2
        "123456"      | "123456"     | "123456"      | UUID.randomUUID() | "200"           | 1
        "123456"      | "nast-id"    | "223456"      | UUID.randomUUID() | "401"           | 0
    }
}
