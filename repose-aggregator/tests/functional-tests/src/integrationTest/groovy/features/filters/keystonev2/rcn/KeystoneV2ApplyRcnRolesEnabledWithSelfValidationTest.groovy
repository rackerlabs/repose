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
import scaffold.category.Identity
import spock.lang.Shared
import spock.lang.Unroll

import java.util.regex.Pattern

import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This covers the same query parameter checking tests as the primary functional test KeystoneV2ApplyRcnRolesEnabledTest
 * but with self-validation turned on. It does not include the admin token test nor does it include the retry tests
 * even though at this time, even self-validating tokens are retried after a 401 response for some reason.
 */
@Category(Identity)
class KeystoneV2ApplyRcnRolesEnabledWithSelfValidationTest extends ReposeValveTest {
    private static final Pattern APPLY_RCN_ROLES_QUERY_PARAM = ~$/$/.*\?.*apply_rcn_roles=(?i:true).*/$
    private static final Pattern TOKEN_VALIDATION_PATH = ~$/$/v2.0/tokens/[^/]+/$
    private static final Pattern GET_ENDPOINTS_PATH = ~$/$/v2.0/tokens/[^/]+/endpoints.*/$
    private static final Pattern GET_GROUPS_PATH = ~$/$/v2.0/users/[^/]+/RAX-KSGRP.*/$

    @Shared
    MockIdentityV2Service mockIdentity

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/rcn/rcnenabledwithselfvalidation", params)

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
}
