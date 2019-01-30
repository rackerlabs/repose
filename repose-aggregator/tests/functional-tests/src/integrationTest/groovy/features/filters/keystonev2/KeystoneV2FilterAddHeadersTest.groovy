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
package features.filters.keystonev2

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Ignore

/**
 * Created by jennyvo on 8/28/15.
 *  Verify auth filter will add headers not replace headers.
 */
@Category(Filters)
class KeystoneV2FilterAddHeadersTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/multitenantheader", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.resetDefaultParameters()
    }

    // REP-2464: Auth filter should add headers not replace headers
    // Note: V2 x-pp-user using username
    def "Verify V2 Auth filter should add headers instead of replace headers"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenantid = "12345"
            client_tenantid2 = "nast-id"
            service_admin_role = "not-admin"
        }

        when:
        "User passes a request through repose with tenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/12345",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                        'x-tenant-id' : 'repose-add-tenant',
                        'x-pp-groups' : 'repose-test-add-group',
                        'x-pp-user'   : 'repose-test-add-user',
                        'x-roles'  : 'test-add-role'])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains("12345")
        (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains("nast-id")
        (mc.handlings[0].request.headers.findAll("x-tenant-id").toString()).contains("repose-add-tenant")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("0")
        (mc.handlings[0].request.headers.findAll("x-pp-groups").toString()).contains("repose-test-add-group")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("not-admin")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("compute:admin")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("object-store:admin")
        (mc.handlings[0].request.headers.findAll("x-roles").toString()).contains("test-add-role")
        (mc.handlings[0].request.headers.findAll("x-pp-user").toString()).contains("repose-test-add-user")
        (mc.handlings[0].request.headers.findAll("x-pp-user").toString()).contains("username")
    }

    // REP-2464: Auth filter should add headers not replace headers
    def "Verify with impersonation, repose should add x-impersonator-roles headers"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = 123456
            client_tenantid = "12345"
            impersonate_name = "impersonator_name"
            impersonate_id = "567"
        }

        when: "User passes a request with impersonation through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/12345/",
                method: 'GET',
                headers: [
                        'content-type'        : 'application/json',
                        'X-Auth-Token'     : fakeIdentityV2Service.client_token,
                        'x-impersonator-roles': 'repose-test'
                ]
        )

        then: "repose should add X-Impersonator-Name and X-Impersonator-Id"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Name") == fakeIdentityV2Service.impersonate_name
        mc.handlings[0].request.headers.getFirstValue("X-Impersonator-Id") == fakeIdentityV2Service.impersonate_id
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("Racker")
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("object-store:admin")
        (mc.handlings[0].request.headers.findAll("x-impersonator-roles").toString()).contains("repose-test")
    }
}
