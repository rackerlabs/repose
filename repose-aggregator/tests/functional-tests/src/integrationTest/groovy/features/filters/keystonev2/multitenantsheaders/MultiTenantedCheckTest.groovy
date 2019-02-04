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
 * Created by jennyvo on 8/19/14.
 */
@Category(Identity)
class MultiTenantedCheckTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service

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

    @Unroll("when passing #requestTenant with setting #defaultTenant #serviceRespCode")
    def "When authenticate user with tenanted client-mapping matching more than one from tenant list"() {
        given:
        fakeIdentityV2Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_tenantid = defaultTenant
            client_tenantid2 = "nast-id"
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
            assert mc.handlings[0].request.headers.findAll('x-tenant-id').get(0).contains(requestTenant)
        }

        where:
        defaultTenant | requestTenant  | authResponseCode | clientToken       | serviceRespCode
        "123456"      | "123456"       | "200"            | UUID.randomUUID() | "200"
        "123456"      | "nast-id"      | "200"            | UUID.randomUUID() | "200"
        "123456"      | "no-a-nast-id" | "200"            | UUID.randomUUID() | "401"
        "900000"      | "nast-id"      | "200"            | UUID.randomUUID() | "200"
        "nast-id"     | "nast-id"      | "200"            | UUID.randomUUID() | "200"
        "900000"      | "900000"       | "200"            | ''                | "401"
    }
}
