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
package features.filters.apivalidator

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.XmlParsing
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/3/14.
 */
@Category(XmlParsing)
class ApiValidatorDelegatingWAuthTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable/withauth", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def setup() {
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    @Unroll("Sending request with roles: #roles, method: #method")
    def "when using api validate with delegate and auth client"() {

        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/a",
                method: method,
                headers: [
                        'x-roles'     : roles,
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        roles                      | method   | responseCode | delegateMsg
        "raxrole-test1"            | "GET"    | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{a};q=0.5"
        "raxrole-test1,a:observer" | "POST"   | "200"        | "status_code=405`component=api-validator`message=Bad method: POST. The Method does not match the pattern: 'GET';q=0.5"
        "raxrole-test1,a:observer" | "DELETE" | "200"        | "status_code=405`component=api-validator`message=Bad method: DELETE. The Method does not match the pattern: 'GET';q=0.5"
        "raxrole-test1,a:admin"    | "PUT"    | "200"        | "status_code=405`component=api-validator`message=Bad method: PUT. The Method does not match the pattern: 'DELETE|GET|POST';q=0.5"
    }

    @Unroll("Sending request with roles: #roles and admin resp: #authresp")
    def "when failing to authenticate admin client"() {

        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        if (authresp != 200) {
            fakeIdentityService.validateTokenHandler = {
                tokenId, tenantid, request ->
                    new Response(authresp)
            }
        }
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/a/123",
                method: method,
                headers: [
                        'x-roles'     : roles,
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        roles                      | method | authresp | responseCode
        "raxrole-test1"            | "GET"  | 401      | "500"
        "raxrole-test1,a:observer" | "POST" | 404      | "401"
    }
}
