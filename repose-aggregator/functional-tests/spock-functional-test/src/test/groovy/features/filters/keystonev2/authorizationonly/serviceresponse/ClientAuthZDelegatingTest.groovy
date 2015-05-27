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
package features.filters.keystonev2.authorizationonly.serviceresponse

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/11/14.
 * check delegating option for authz
 */
class ClientAuthZDelegatingTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorizationonly/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorizationonly/delegating", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }


    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll("When user role #roles not in ignore-tenant-role list")
    def "Check non-tenanted AuthZ with #roles and expected response code #respcode"() {
        given:
        fakeIdentityService.with {
            client_token = "rackerButts"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "456"
        }

        def reqHeaders =
                [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-roles'     : roles
                ]
        def authDelegatingMsg = 'status_code=403.component=client-authorization.message=.*\\"http:\\/\\/\\w+([-|:\\d]+)\\/\\"\\.\\s+User not authorized to access service.;q=0.3'

        when: "User passes a request through repose with role #roles"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/serrrrrrrr", method: 'GET',
                headers: reqHeaders)

        then: "User with #roles should get response code #respcode"
        mc.receivedResponse.code == respcode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.getFirstValue("x-delegated") =~ authDelegatingMsg

        where: "User with #roles expect response code #respcode"
        roles | respcode
        'user-admin' | "200"
        'non-admin' | "200"
        null | "200"
        '' | "200"
        'openstack%2Cadmin' | '200'
        'admin%20' | '200'
    }

    @Unroll("When #method request with URL is not in the user's service list")
    def "When user requests a URL that is not in the user's service list repose should forward 403 FORBIDDEN to origin service"() {

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = token
        fakeIdentityService.originServicePort = 99999
        def strregex = 'status_code=403.component=client-authorization.message=.*\\"http:\\/\\/\\w+([-|:\\d]+)\\/\\"\\.\\s+User not authorized to access service.;q=0.3'

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + token + "/ss", method: method, headers: ['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "Repose should forward to origin service with failure message"
        foundLogs.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.getFirstValue("x-delegated") =~ strregex

        where:
        method << ["GET", "POST", "PUT", "PATCH", "DELETE"]
    }

    @Unroll("Identity Service Broken Admin Call: #adminBroken Broken Token Endpoints Call: #endpointsBroken Error Code: #errorCode")
    def "When Auxiliary service is broken for Service Endpoints call"() {

        given: "When Calls to Auth Return bad responses"

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
        }
        if (adminBroken) {
            fakeIdentityService.generateTokenHandler = { request, xml -> return new Response(errorCode) }
        }
        if (endpointsBroken) {
            fakeIdentityService.getEndpointsHandler = { tokenId, request, xml -> return new Response(errorCode) }
        }
        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then:
        "User should receive a " + expectedCode + "response"
        mc.receivedResponse.code == expectedCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        adminBroken | endpointsBroken | errorCode | expectedCode | delegatingMsg
        true        | false           | 400       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 401       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 402       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 403       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 404       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 413       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 429       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 500       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 501       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 502       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        true        | false           | 503       | "200"        | "status_code=403.component=client-authorization.message=.*;q=0.3"
        false       | true            | 400       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 401       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 402       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 403       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 404       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 413       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 429       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 500       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 501       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 502       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"
        false       | true            | 503       | "200"        | "status_code=500.component=client-authorization.message=.*;q=0.3"

    }
}

