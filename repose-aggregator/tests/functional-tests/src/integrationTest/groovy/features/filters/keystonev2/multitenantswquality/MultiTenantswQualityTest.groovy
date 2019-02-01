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
package features.filters.keystonev2.multitenantswquality

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 1/13/15.
 */
@Category(Filters)
class MultiTenantswQualityTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def static params

    def setupSpec() {

        deproxy = new Deproxy()

        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/multitenantswquality", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    @Unroll("#defaultTenant, #secondTenant, #requestTenant ")
    def "When user token have multi-tenant will retrieve all tenants in the header"() {
        given:
        fakeIdentityService.with {
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
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains(defaultTenant + ";q=0.9")
            if (!secondTenant.equals(defaultTenant)) {
                assert (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains(secondTenant + ";q=0.7")
            }
        }

        where:
        defaultTenant | secondTenant | requestTenant | clientToken       | serviceRespCode | numberTenants
        "123456"      | "nast-id"    | "123456"      | UUID.randomUUID() | "200"           | 2
        "123456"      | "nast-id"    | "nast-id"     | UUID.randomUUID() | "200"           | 2
        "123456"      | "123456"     | "123456"      | UUID.randomUUID() | "200"           | 1
        "123456"      | "nast-id"    | "223456"      | UUID.randomUUID() | "401"           | 0
    }

    @Unroll("Request Tenant: #requestTenant")
    def "With legacy xsd namespace: when user token have multi-tenant will retrieve all tenants in the header"() {
        given:
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/multitenantswquality/oldnamespace", params, /*sleepTime*/ 25)
        fakeIdentityService.with {
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
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").get(0).split(",").size() == numberTenants
            assert (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains(defaultTenant + ";q=0.3")
            if (!requestTenant.equals(defaultTenant)) {
                // base on default quality for tenant from url and roles
                assert (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains(secondTenant + ";q=0.7")
                assert (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains(secondTenant + ";q=0.5")
            }
        }

        where:
        defaultTenant | secondTenant | requestTenant | clientToken       | serviceRespCode | numberTenants
        "123456"      | "nast-id"    | "123456"      | UUID.randomUUID() | "200"           | 4
        "123456"      | "nast-id"    | "nast-id"     | UUID.randomUUID() | "200"           | 4
        "123456"      | "nast-id"    | "NAST-ID"     | UUID.randomUUID() | "401"           | 0
        "123456"      | "nast-id"    | "123457"      | UUID.randomUUID() | "401"           | 0
    }
}

