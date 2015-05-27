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

class NonTenantedNonDelegableNoServiceAdminRolesTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/nontenantednondelegablenoserviceadminroles", params)
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

    def "when authenticating user in non tenanted and non delegable mode without service admin roles - pass"() {

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = "tenant456"
            client_userid = "tenant456"
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/tenant123/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request from repose should contain proper headers"
        def requestFromRepose = mc.getHandlings()[0].getRequest()
        requestFromRepose.getHeaders()["x-tenant-id"] == fakeIdentityService.client_tenant
        requestFromRepose.getHeaders()["x-user-id"] == fakeIdentityService.client_userid
        requestFromRepose.getHeaders()["x-default-region"] == "the-default-region"
        requestFromRepose.path == "/servers/tenant123/"
        requestFromRepose.getHeaders()["content-type"] == "application/json"
        requestFromRepose.getHeaders()["x-auth-token"] == fakeIdentityService.client_token
        requestFromRepose.getHeaders()["x-tenant-name"] == fakeIdentityService.client_tenant
        requestFromRepose.getHeaders()["x-pp-user"] == "username;q=1.0"
        requestFromRepose.getHeaders()["x-roles"].contains("compute:default")
        requestFromRepose.getHeaders()["x-pp-groups"] == "0;q=1.0"
        mc.receivedResponse.code == "200"
    }

    @Unroll("tenant: #requestTenant with response from identity (tenant: #responseTenant, identity response: #authResponseCode and group response: #groupResponseCode)")
    def "when authenticating user in non tenanted and non delegable mode without service admin roles - fail"() {

        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            client_userid = responseTenant
        }

        if (authResponseCode != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(authResponseCode)
            }
        }

        if (groupResponseCode != 200) {
            fakeIdentityService.getGroupsHandler = {
                userId, request, xml ->
                    new Response(groupResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Auth should fail and stop the chain"
        mc.receivedResponse.code == responseCode

        where:
        requestTenant | responseTenant | authResponseCode | responseCode | groupResponseCode | clientToken
        613           | 613            | 500              | "500"        | 200               | UUID.randomUUID()
        614           | 614            | 404              | "401"        | 200               | UUID.randomUUID()
        615           | 615            | 200              | "500"        | 404               | UUID.randomUUID()
        616           | 616            | 200              | "500"        | 500               | UUID.randomUUID()
        ""            | 612            | 200              | "401"        | 200               | ""

    }

}
