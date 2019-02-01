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
 * This is the primary functional test suite for the Apply RCN Roles feature. Other functional tests cover unique cases
 * and try to not cover essentially the same cases already covered here.
 */
@Category(Filters)
class KeystoneV2ApplyRcnRolesEnabledTest extends ReposeValveTest {
    private static final Pattern APPLY_RCN_ROLES_QUERY_PARAM = ~$/$/.*\?.*apply_rcn_roles=(?i:true).*/$
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
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/rcn/rcnenabled", params)

        mockIdentity = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort, 'identity service', null, mockIdentity.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        mockIdentity.resetHandlers()
    }

    @Unroll
    def "When Repose calls Identity to #requestPurpose, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to validate the token"
        messageChain.orphanedHandlings.find { it.request.path ==~ pathRegex }

        and: "the token validation request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.find { it.request.path ==~ pathRegex }.request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM

        where:
        requestPurpose       | pathRegex
        "validate the token" | TOKEN_VALIDATION_PATH
        "get the endpoints"  | GET_ENDPOINTS_PATH
    }

    def "When Repose calls Identity to get the groups, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/get/groups", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to get the groups"
        messageChain.orphanedHandlings.find { it.request.path ==~ GET_GROUPS_PATH }

        and: "the get groups request to Identity did not include the query parameter set to true (case insensitive on the value)"
        !(messageChain.orphanedHandlings.find { it.request.path ==~ GET_GROUPS_PATH }
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM)
    }

    def "When Repose calls Identity to get the admin token, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        and: "the admin token will have to be requested again"
        mockIdentity.validateTokenHandler = { tokenId, tenantId, request ->
            mockIdentity.admin_token = UUID.randomUUID().toString()
            mockIdentity.validateTokenHandler = mockIdentity.&validateToken
            return new Response(SC_UNAUTHORIZED)
        }

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/get/admin/token", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to get the admin token"
        messageChain.orphanedHandlings.find { it.request.path ==~ GET_TOKEN_PATH }

        and: "the get admin token request to Identity did not include the query parameter set to true (case insensitive on the value)"
        !(messageChain.orphanedHandlings.find { it.request.path ==~ GET_TOKEN_PATH }
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM)
    }

    @Unroll
    def "When Repose has to retry the call to Identity to #requestPurpose, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        and: "the admin token will have expired between the time it will be requested and the time it will be used for the validate token call"
        (handlerSetter as Closure) { unused1, unused2, unused3 = null ->
            mockIdentity.admin_token = UUID.randomUUID().toString()
            handlerSetter(defaultHandler)
            return new Response(SC_UNAUTHORIZED)
        }

        when: "a request is sent"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/retry/identity/call", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "the request was sent to Identity twice"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ pathRegex }.size() == 2

        and: "the retried request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ pathRegex }[1]
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM

        where:
        requestPurpose       | pathRegex             | handlerSetter                         | defaultHandler
        "validate the token" | TOKEN_VALIDATION_PATH | mockIdentity.&setValidateTokenHandler | mockIdentity.&validateToken
        "get the endpoints"  | GET_ENDPOINTS_PATH    | mockIdentity.&setGetEndpointsHandler  | mockIdentity.&listEndpointsForToken
    }

    def "When Repose has to retry the call to Identity to get the groups, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        mockIdentity.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': mockIdentity.client_token]

        and: "the admin token will have expired between the time it will be requested and the time it will be used for the get groups call"
        mockIdentity.getGroupsHandler = { tokenId, request ->
            mockIdentity.admin_token = UUID.randomUUID().toString()
            mockIdentity.getGroupsHandler = mockIdentity.&listUserGroups
            return new Response(SC_UNAUTHORIZED)
        }

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/retry/get/groups", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "the request to get the groups from Identity was sent twice"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ GET_GROUPS_PATH }.size() == 2

        and: "the retried get groups request to Identity did not include the query parameter set to true (case insensitive on the value)"
        !(messageChain.orphanedHandlings.findAll { it.request.path ==~ GET_GROUPS_PATH }[1]
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM)
    }
}
