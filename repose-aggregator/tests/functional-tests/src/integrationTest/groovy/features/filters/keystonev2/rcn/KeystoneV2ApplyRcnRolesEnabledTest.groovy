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

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Shared

import java.util.regex.Pattern

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

class KeystoneV2ApplyRcnRolesEnabledTest extends ReposeValveTest {
    private static final Pattern APPLY_RCN_ROLES_QUERY_PARAM = ~$/$/.*\?.*apply_rcn_roles=(?i:true).*/$
    private static final Pattern TOKEN_VALIDATION_PATH = ~$/$/v2.0/tokens/[^/]+/$
    private static final Pattern GET_ENDPOINTS_PATH = ~$/$/v2.0/tokens/[^/]+/endpoints.*/$
    private static final Pattern GET_GROUPS_PATH = ~$/$/v2.0/users/[^/]+/RAX-KSGRP.*/$
    private static final Pattern GET_TOKEN_PATH = ~$/$/v2.0/tokens(\?.*)?/$

    @Shared
    MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/rcn/rcnenabled", params)

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    def "When Repose calls Identity to validate the token, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/validate/token", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to validate the token"
        messageChain.orphanedHandlings.find { it.request.path ==~ TOKEN_VALIDATION_PATH }

        and: "the token validation request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.find { it.request.path ==~ TOKEN_VALIDATION_PATH }
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM
    }

    def "When Repose calls Identity to get the endpoints, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/get/endpoints", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "a request was sent to Identity to get the endpoints"
        messageChain.orphanedHandlings.find { it.request.path ==~ GET_ENDPOINTS_PATH }

        and: "the get endpoints request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.find { it.request.path ==~ GET_ENDPOINTS_PATH }
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM
    }

    def "When Repose calls Identity to get the groups, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

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
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        and: "the admin token will have to be requested again"
        def initialAdminToken = fakeIdentityV2Service.admin_token
        fakeIdentityV2Service.validateTokenHandler = { String tokenId, String tenantId, Request request ->
            if (fakeIdentityV2Service.admin_token == initialAdminToken) {
                fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
                return new Response(SC_UNAUTHORIZED)
            } else {
                return fakeIdentityV2Service.validateToken(tokenId, tenantId, request)
            }
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

    def "When Repose has to retry the call to Identity to validate the token, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        and: "the admin token will have expired between the time it will be requested and the time it will be used for the validate token call"
        def initialAdminToken = fakeIdentityV2Service.admin_token
        fakeIdentityV2Service.validateTokenHandler = { String tokenId, String tenantId, Request request ->
            if (fakeIdentityV2Service.admin_token == initialAdminToken) {
                fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
                return new Response(SC_UNAUTHORIZED)
            } else {
                return fakeIdentityV2Service.validateToken(tokenId, tenantId, request)
            }
        }

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/retry/validate/token", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "the request to validate the token with Identity was sent twice"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ TOKEN_VALIDATION_PATH }.size() == 2

        and: "the retried token validation request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ TOKEN_VALIDATION_PATH }[1]
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM
    }

    def "When Repose has to retry the call to Identity to get the endpoints, it includes the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        and: "the admin token will have expired between the time it will be requested and the time it will be used for the get endpoints call"
        def initialAdminToken = fakeIdentityV2Service.admin_token
        fakeIdentityV2Service.getEndpointsHandler = { String tokenId, Request request ->
            if (fakeIdentityV2Service.admin_token == initialAdminToken) {
                fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
                return new Response(SC_UNAUTHORIZED)
            } else {
                return fakeIdentityV2Service.listEndpointsForToken(tokenId, request)
            }
        }

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/test/retry/validate/token", headers: headers)

        then: "the client response was successful"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "the request to get the endpoints from Identity was sent twice"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ GET_ENDPOINTS_PATH }.size() == 2

        and: "the retried get endpoints request to Identity included the query parameter set to true (case insensitive on the value)"
        messageChain.orphanedHandlings.findAll { it.request.path ==~ GET_ENDPOINTS_PATH }[1]
            .request.path ==~ APPLY_RCN_ROLES_QUERY_PARAM
    }

    def "When Repose has to retry the call to Identity to get the groups, it does not include the apply_rcn_role query param"() {
        given: "a new user (token and id) to ensure the calls to Identity are not handled by the Akka cache"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_userid = UUID.randomUUID().toString()
        }
        def headers = ['X-Auth-Token': fakeIdentityV2Service.client_token]

        and: "the admin token will have expired between the time it will be requested and the time it will be used for the get groups call"
        def initialAdminToken = fakeIdentityV2Service.admin_token
        fakeIdentityV2Service.getGroupsHandler = { String userId, Request request ->
            if (fakeIdentityV2Service.admin_token == initialAdminToken) {
                fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
                return new Response(SC_UNAUTHORIZED)
            } else {
                return fakeIdentityV2Service.listUserGroups(userId, request)
            }
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
