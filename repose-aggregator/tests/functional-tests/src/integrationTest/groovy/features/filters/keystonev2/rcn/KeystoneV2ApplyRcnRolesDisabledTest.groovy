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

package features.filters.keystonev2.rcn

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Shared
import spock.lang.Unroll

import java.util.regex.Pattern

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

/**
 * Verifies calls to Identity when the Apply RCN Roles functionality is explicitly disabled in config.
 */
@Category(Filters)
class KeystoneV2ApplyRcnRolesDisabledTest extends ReposeValveTest {
    private static final String APPLY_RCN_ROLES = "apply_rcn_roles"
    private static final Pattern TOKEN_VALIDATION_PATH = ~$/$/v2.0/tokens/[^/]+/$
    private static final Pattern GET_ENDPOINTS_PATH = ~$/$/v2.0/tokens/[^/]+/endpoints.*/$
    private static final Pattern GET_GROUPS_PATH = ~$/$/v2.0/users/[^/]+/RAX-KSGRP.*/$
    private static final Pattern GET_TOKEN_PATH = ~$/$/v2.0/tokens(\?.*)?/$

    @Shared
    MockIdentityV2Service mockIdentity

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/rcn/rcndisabled", params)

        mockIdentity = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort, 'identity service', null, mockIdentity.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        mockIdentity.resetHandlers()
    }

    @Unroll
    def "When Repose calls Identity to #requestPurpose, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        when: "a request is sent"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity for this specific purpose"
        messageChain.orphanedHandlings.find { it.request.path ==~ pathRegex }

        and: "the request to Identity did not include the query parameter string at all"
        !(messageChain.orphanedHandlings.find { it.request.path ==~ pathRegex }.request.path.contains(APPLY_RCN_ROLES))

        where:
        requestPurpose       | pathRegex
        "validate the token" | TOKEN_VALIDATION_PATH
        "get the endpoints"  | GET_ENDPOINTS_PATH
        "get the groups"     | GET_GROUPS_PATH
    }

    @Unroll
    def "When Repose retries the call to Identity to #requestPurpose (expired admin token), it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        and: "the admin token will have to be requested again due to expiring right before the specified request"
        (handlerSetter as Closure) { unused1, unused2, unused3 = null ->
            mockIdentity.admin_token = UUID.randomUUID().toString()
            handlerSetter(defaultHandler)
            return new Response(SC_UNAUTHORIZED)
        }

        when: "a request is sent"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity twice"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ pathRegex }.size() == 2

        and: "none of the requests with the specified path to Identity contained the query parameter string"
        !(messageChain.orphanedHandlings
            .findAll { it.request.path ==~ pathRegex }
            .any { it.request.path.contains(APPLY_RCN_ROLES) })

        where:
        requestPurpose       | pathRegex             | handlerSetter                         | defaultHandler
        "validate the token" | TOKEN_VALIDATION_PATH | mockIdentity.&setValidateTokenHandler | mockIdentity.&validateToken
        "get the endpoints"  | GET_ENDPOINTS_PATH    | mockIdentity.&setGetEndpointsHandler  | mockIdentity.&listEndpointsForToken
        "get the groups"     | GET_GROUPS_PATH       | mockIdentity.&setGetGroupsHandler     | mockIdentity.&listUserGroups
    }

    def "When Repose calls Identity to get an admin token, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        and: "the admin token has already been requested (either here or in a prior test)"
        deproxy.makeRequest(url: reposeEndpoint + "/test/ignored", headers: ['X-Auth-Token': UUID.randomUUID().toString()])
        sleep(600) // Akka cache, 500 ms + padding

        and: "the admin token will have to be requested again (and will be done only once in this message chain)"
        mockIdentity.validateTokenHandler = { unused1, unused2, unused3 ->
            mockIdentity.admin_token = UUID.randomUUID().toString()
            mockIdentity.validateTokenHandler = mockIdentity.&validateToken
            return new Response(SC_UNAUTHORIZED)
        }

        when: "a request is sent"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to get the admin token only once"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ GET_TOKEN_PATH }.size() == 1

        and: "the get admin token request to Identity did not include the query parameter set to true (case insensitive on the value)"
        !(messageChain.orphanedHandlings.find { it.request.path ==~ GET_TOKEN_PATH }.request.path.contains(APPLY_RCN_ROLES))
    }
}
