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
class Keystonev2NonTenantAndIso8559Test extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service
    def static params = [:]

    def setupSpec() {

        deproxy = new Deproxy()
        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/tenantandroleshandling", params)

        originEndpoint = deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        identityEndpoint = deproxy.addEndpoint(params.identityPort,
                'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "Non Tenant check will send all roles"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = "mytenant"
            client_tenantname = "mytenantname"
            client_userid = "12345"
            service_admin_role = "repose:test"
        }

        when: "User passes a request through repose with valid token"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/test", method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "should return all roles"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("compute:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("object-store:admin")
        mc.handlings[0].request.headers.findAll("x-roles").toString().contains("repose:test")
    }

    @Unroll("#tenantId will be forwarded without modification as a tenant ID")
    def "ISO-8559-1 characters in tenant IDs will be forwarded without modification"() {
        given: "escaping to accommodate the JSON envelope"
        String escapedTenantId = tenantId.replaceAll((0x09 as char) as String, '\\\\t')
        if ([0x22, 0x5C].collect { (it as char) as String }.contains(tenantId)) {
            escapedTenantId = '\\' + tenantId
        }

        and:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = escapedTenantId
        }

        when:
        MessageChain mc = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-tenant-id").contains(tenantId)

        where:
        tenantId << [
            // All visible characters from ISO-8859-1
            *(0x21..0x7E).collect { (it as char) as String },
            // Space in the tenant ID
            'a' + (0x20 as char) + 'a',
            // Horizontal tab in the tenant ID
            'a' + (0x09 as char) + 'a',
        ]
    }

    @Unroll("#tenantId will be forwarded incorrectly as a tenant ID")
    def "ISO-8559-1 characters in the range 0x80-0xFF in tenant IDs will be forwarded incorrectly"() {
        given:
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantId
        }

        when:
        MessageChain mc = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.findAll("x-tenant-id").contains(tenantId)

        where:
        tenantId <<
            // All non-ASCII (thus, opaque) octets
            (0x80..0xFF).collect { (it as char) as String }
    }
}
