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
package features.filters.keystonev2.tenantvalidation

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * In this scenario we are testing when the configuration requires the client to be tenanted and allows delegable
 * passthrough
 *
 * What that means is the following:
 * - request must specify a tenant id for the client to validate against
 * - request can be delegable, which means that the authentication can be delegated to origin service.
 * @return
 */
class ClientAuthNRemoveTenantTenantedDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/tenanteddelegable", params)
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


    @Unroll("tenant: #requestTenant with mismatching response tenant id (#responseTenant) and non-service admin roles and response from identity: #authResponseCode")
    def "when authenticating user in tenanted and delegable mode and client-mapping matching - fail"() {

        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = "not-admin"
        }

        if (authResponseCode != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(authResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token + requestTenant
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1

        where:
        requestTenant | responseTenant | authResponseCode | responseCode | delegatingMsg
        200           | 201            | 500              | "200"        | "status_code=500.component=client-auth-n.message=Failure in Auth-N filter.;q=0.7"
        202           | 203            | 404              | "200"        | "status_code=401.component=client-auth-n.message=Failure in Auth-N filter.;q=0.7"
        204           | 205            | 200              | "200"        | "status_code=401.component=client-auth-n.message=Failure in Auth-N filter.;q=0.7"
    }

    @Unroll("tenant: #requestTenant with identity returning HTTP 200 response with tenant id (#responseTenant), role (#serviceAdminRole)")
    def "when authenticating user in tenanted and delegable mode and client-mapping matching - pass"() {

        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': "${fakeIdentityService.client_token}$requestTenant"
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.contains("x-auth-token")
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == "Confirmed"
        request2.headers.getFirstValue("x-authorization") == "Proxy " + requestTenant

        where:
        requestTenant | responseTenant | serviceAdminRole      | responseCode
        206           | 206            | "not-admin"           | "200"
        207           | 207            | "not-admin"           | "200"
        208           | 208            | "service:admin-role1" | "200"
        208           | 209            | "service:admin-role1" | "200"
    }

}
