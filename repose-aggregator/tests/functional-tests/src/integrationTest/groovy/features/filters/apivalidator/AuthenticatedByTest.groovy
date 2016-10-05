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

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static org.junit.Assert.assertEquals

/**
 * A test to verify that a user can validate authentication mechanisms via api-checker.
 */
class AuthenticatedByTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/apivalidator/common', params)
        repose.configurationProvider.applyConfigs('features/filters/apivalidator/authenticatedBy', params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("A #method call to /v0/safe with the headers #headers should be Unauthorized.")
    def 'All calls to the /v0/safe resource without the correct authentication method should be Unauthorized.'() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/safe',
                method: method,
                headers: headers
        )

        then:
        assertEquals(SC_UNAUTHORIZED, Integer.valueOf(messageChain.getReceivedResponse().getCode()))

        where:
        [method, headers] << [
                ['POST', 'GET', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE'],
                [null, ['X-Authenticated-By': 'Some-Auth, MFA'], ['X-Authenticated-By': 'Some-Auth, Any'], ['X-Authenticated-By': 'Some-Auth, Super Duper']]
        ].combinations()
    }

    @Unroll("A #method call to /v0/safe with the headers [X-Authenticated-By: Some-Auth, password] should be Ok.")
    def 'All calls to the /v0/safe resource with the correct authentication method should be Ok.'() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/safe',
                method: method,
                headers: ['X-Authenticated-By': 'Some-Auth, password']
        )

        then:
        assertEquals(SC_OK, Integer.valueOf(messageChain.getReceivedResponse().getCode()))

        where:
        method << ['POST', 'GET', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE']
    }

    @Unroll("A #method call to /v0/#target with the headers #headers should be Ok.")
    def 'All calls to the /v0/open and /v0/none resources should be Ok.'() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(
                url: reposeEndpoint + "/v0/$target",
                method: method,
                headers: headers
        )

        then:
        assertEquals(SC_OK, Integer.valueOf(messageChain.getReceivedResponse().getCode()))

        where:
        [target, method, headers] << [
                ['open', 'none'],
                ['POST', 'GET', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE'],
                [null, ['X-Authenticated-By': 'Some-Auth, password'], ['X-Authenticated-By': 'Some-Auth, MFA'], ['X-Authenticated-By': 'Some-Auth, Any'], ['X-Authenticated-By': 'Some-Auth, Super Duper']]
        ].combinations()
    }

    @Unroll("A #method call to /v0/vary with the headers #headers should be #responseCode.")
    def 'Calls to the /v0/vary resource should vary.'() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/vary',
                method: method,
                headers: headers
        )

        then:
        assertEquals(responseCode, Integer.valueOf(messageChain.getReceivedResponse().getCode()))

        where:
        method   | headers                                                | responseCode
        'POST'   | null                                                   | SC_UNAUTHORIZED
        'POST'   | ['X-Authenticated-By': 'Some-Auth, password']          | SC_OK
        'POST'   | ['X-Authenticated-By': 'Some-Auth, MFA']               | SC_OK
        'POST'   | ['X-Authenticated-By': 'Some-Auth, Any']               | SC_UNAUTHORIZED
        'POST'   | ['X-Authenticated-By': 'Some-Auth, Super Duper']       | SC_UNAUTHORIZED
        'GET'    | null                                                   | SC_OK
        'GET'    | ['X-Authenticated-By': 'Some-Auth, password']          | SC_OK
        'GET'    | ['X-Authenticated-By': 'Some-Auth, MFA']               | SC_OK
        'GET'    | ['X-Authenticated-By': 'Some-Auth, Any']               | SC_OK
        'GET'    | ['X-Authenticated-By': 'Some-Auth, Super Duper']       | SC_OK
        'PUT'    | null                                                   | SC_UNAUTHORIZED
        'PUT'    | ['X-Authenticated-By': 'Some-Auth, password']          | SC_UNAUTHORIZED
        'PUT'    | ['X-Authenticated-By': 'Some-Auth, MFA']               | SC_OK
        'PUT'    | ['X-Authenticated-By': 'Some-Auth, Any']               | SC_UNAUTHORIZED
        'PUT'    | ['X-Authenticated-By': 'Some-Auth, Super Duper']       | SC_UNAUTHORIZED
        'PATCH'  | null                                                   | SC_OK
        'PATCH'  | ['X-Authenticated-By': 'Some-Auth, password']          | SC_OK
        'PATCH'  | ['X-Authenticated-By': 'Some-Auth, MFA']               | SC_OK
        'PATCH'  | ['X-Authenticated-By': 'Some-Auth, Any']               | SC_OK
        'PATCH'  | ['X-Authenticated-By': 'Some-Auth, Super Duper']       | SC_OK
        'DELETE' | null                                                   | SC_UNAUTHORIZED
        'DELETE' | ['X-Authenticated-By': 'Some-Auth, password']          | SC_UNAUTHORIZED
        'DELETE' | ['X-Authenticated-By': 'Some-Auth, MFA']               | SC_UNAUTHORIZED
        'DELETE' | ['X-Authenticated-By': 'Some-Auth, Any']               | SC_UNAUTHORIZED
        'DELETE' | ['X-Authenticated-By': 'Some-Auth, Super Duper']       | SC_OK
    }

    @Unroll("A #method call to /v0/vary with the headers #headers should be Not Allowed.")
    def 'Disallowed calls to the /v0/vary resource should be Not Allowed.'() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/vary',
                method: method,
                headers: headers
        )

        then:
        assertEquals(SC_METHOD_NOT_ALLOWED, Integer.valueOf(messageChain.getReceivedResponse().getCode()))

        where:
        [method, headers] << [
                ['HEAD', 'OPTIONS', 'TRACE'],
                [null, ['X-Authenticated-By': 'Some-Auth, password'], ['X-Authenticated-By': 'Some-Auth, MFA'], ['X-Authenticated-By': 'Some-Auth, Any'], ['X-Authenticated-By': 'Some-Auth, Super Duper']]
        ].combinations()
    }
}
