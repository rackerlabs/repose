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
package features.filters.keystonev2.tenantandroleshandling

import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/10/16.
 *  New Tenant and Roles handling
 *  Cases:
 *      1, forward all roles if not in tenanted mode
 *      2, forward all roles if tenanted mode and role legacy mode enabled
 *      3, forward all roles if tenanted mode, roles legacy mode disabled, but pre-authorize role set for user
 *      4, forward only role(s) that tenant associated with. if tenanted mode and role legacy mode disabled
 */
class Keystonev2TenantWithRolesHandlingTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service
    def static params = [:]

    def setupSpec() {
        deproxy = new Deproxy()
        params = properties.defaultTemplateParams

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def cleanup() {
        repose?.stop()
    }

    def "Tenanted with role handling in legacy mode"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "12345"
            client_tenantname = "mytenantname"
            client_userid = "12345"
            service_admin_role = "repose:test"
        }

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/tenantandroleshandling/tenantedlegacymode", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/12345", method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should return all roles"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("compute:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("object-store:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("repose:test")
    }

    @Unroll
    def "Tenanted with role handling; tenant: #tenantid should return role: #returnroles"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantid2 = "12345"
            client_tenantname = "mytenantname"
            client_userid = "12345"
            service_admin_role = "repose:test"
        }

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/tenantandroleshandling/tenantedwrole", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/" + tenantid, method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should return only role(s) that tenant associated with"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains(returnroles[0])
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains(returnroles[1])
        !mc.handlings[0].request.headers.findAll("x-roles").toString().contains(notreturnrole)

        where:
        tenantid   | returnroles                           | notreturnrole
        "mytenant" | ["compute:admin", "repose:test"]      | "object-store:admin"
        "12345"    | ["object-store:admin", "repose:test"] | "compute:admin"
    }

    @Unroll
    def "Tenanted with pre-authorize role handling; tenant: #tenantid"() {
        given: "configuration"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantid2 = "12345"
            client_tenantname = "mytenantname"
            client_userid = "12345"
            service_admin_role = "repose:test"
        }

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/tenantandroleshandling/tenantedwpreauthorizerole", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/" + tenantid, method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should return all roles"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("compute:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("object-store:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("repose:test")

        where:
        tenantid << ["12345", "mytenant"]
    }
}
