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
package features.filters.clientauthn.tenantvalidation

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/24/15.
 */
class StripTenantPrefixTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/striptenantprefix", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup() {
        fakeIdentityService.resetHandlers()
    }

    @Unroll("request tenant: #requestTenant and response tenant: #responseTenant - #responseCode")
    def "Strip Tenant Prefix enabled"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = "not-admin"
            client_userid = requestTenant
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in non-admin service role"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/accounts/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode

        where:
        requestTenant | responseTenant | responseCode
        "1234"        | "hybrid:1234"  | "200"
        "1234"        | "foo:1234"     | "200"
        "1234"        | "bar-1234"     | "200"
        "1235"        | "1235"         | "200"
        "1234"        | "hybrid:1235"  | "401"
        "hybrid:1235" | "hybrid:1235"  | "401"
        "hybrid:1235" | "1235"         | "401"
        "bar-1234"    | "bar-1234"     | "401"
    }

    @Unroll("ignore roles: #servicerole, request tenant: #requestTenant and response tenant: #responseTenant - #responseCode")
    def "Strip Tenant Prefix enabled with ignore service admin roles"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = servicerole
            client_userid = requestTenant
        }

        when:
        "User passes a request through repose with request tenant: $requestTenant, response tenant: $responseTenant in non-admin service role"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/accounts/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode

        where:
        requestTenant | responseTenant | servicerole           | responseCode
        "1234"        | "hybrid:1234"  | "service:admin-role1" | "200"
        "1234"        | "foo:1234"     | "service:admin-role2" | "200"
        "1234"        | "bar-1234"     | "service:non-admin"   | "200"
        "1234"        | "hybrid:1235"  | "service:admin-role2" | "200"
        "123456"      | "foo:1235"     | "service:admin-role1" | "200"
        "1235"        | "1235"         | "service:admin-role2" | "200"
        "123456"      | "bar-1234"     | "service:admin-role2" | "200"
    }
}
