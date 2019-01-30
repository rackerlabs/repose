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
package features.filters.keystonev2basicauth.wpassword

import org.apache.commons.lang3.RandomStringUtils
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE

/**
 * Created by jennyvo on 1/15/16.
 *  using username/password
 * Update on 01/21/16
 *  - Replace client-auth-n with keystone-v2 filter
 */
@Category(Filters)
class BasicAuthPasswordTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/keystonev2basicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/keystonev2basicauth/wpassword", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true
    }

    def setup() {
        fakeIdentityService.resetHandlers()
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_password = RandomStringUtils.random(8, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
            client_token = UUID.randomUUID().toString()
        }
    }

    def "Retrieve a token for an HTTP Basic authentication header with UserName/password"() {
        given: "the HTTP Basic authentication header containing the User Name and password"
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_password).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/password"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get a token for it"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc.handlings[0].request.headers.getFirstValue(AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(WWW_AUTHENTICATE)
        //**This test tracing header Repose handling request, and the request to identity as part of REP-1704**
        mc.orphanedHandlings.each {
            e ->
                assert e.request.headers.contains("x-trans-id")
                assert e.request.headers.getFirstValue("x-trans-id") == mc.handlings[0].request.headers.getFirstValue("x-trans-id")
        }
        //verify if request send to identity with password credential
        mc.orphanedHandlings.get(0).request.body.contains("passwordCredentials")
        mc.orphanedHandlings.get(0).request.body.contains("password")
        mc.orphanedHandlings.get(0).request.body.contains(fakeIdentityService.client_password)
    }

    def "No HTTP Basic authentication header sent and no token."() {
        when: "the request does not have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain and this configuration will respond with a SC_UNAUTHORIZED (401) and add an HTTP Basic authentication header"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        mc.orphanedHandlings.size() == 0
    }

    def "When the request has an x-auth-token, then still work with client-auth"() {
        given:
        def headers = ['X-Auth-Token': fakeIdentityService.client_token]

        when: "the request already has an x-auth-token header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.getFirstValue(AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(WWW_AUTHENTICATE)
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token")
    }

    def "When the request send with invalid password or username, then will fail to authenticate"() {
        given: "the HTTP Basic authentication header containing the User Name and invalid password"
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":BAD-API-KEY").bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get a token and validate it"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "When the request send with invalid 0 < password < 100 , then will fail to authenticate"() {
        given: "the HTTP Basic authentication header containing the User Name and invalid password"
        def password = RandomStringUtils.random(99, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + password).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "response with 400 bad request"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    // identity currently return 400 bad request for api-key > 100 characters
    def "When the request send with invalid long password > 100 , then will fail to authenticate"() {
        given: "the HTTP Basic authentication header containing the User Name and invalid password"
        def password = RandomStringUtils.random(120, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + password).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "response with 401 Unauthorized"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "When the request send with username > 100 , then will fail to authenticate"() {
        given: "the HTTP Basic authentication header containing the User Name and invalid password"
        def username = RandomStringUtils.random(120, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((username + ":" + "randompassword").bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "response with 401 Unauthorized"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "When identity returns a 403 Forbidden, repose will also return a 403 Forbidden"() {
        given: "the HTTP Basic authentication header containing the User Name and forbidden API key"
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.forbidden_apikey_or_pwd).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "response with 403 Forbidden"
        mc.receivedResponse.code as Integer == SC_FORBIDDEN
        mc.handlings.size() == 0
        !mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "When identity returns a 404 Not Found, repose will return a 401 Unauthorized"() {
        given: "the HTTP Basic authentication header containing the User Name and not found apikey"
        def headers = [
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.not_found_apikey_or_pwd).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "response with 401 Unauthorized"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    // This test was removed due to a current limitation of the MockIdentityService to not differentiate between the two services calling it.
    @Unroll("Sending request with admin response set to HTTP #identityStatusCode")
    def "when failing to authenticate admin client"() {
        given: "the HTTP Basic authentication header containing the User Name and password and the Mock Identity Service's generateTokenHandler"
        fakeIdentityService.with {
            generateTokenHandler = {
                request, xml ->
                    new Response(identityStatusCode, null, null, null)
            }
        }
        def headers = [
                'content-type' : 'application/json',
                (AUTHORIZATION): 'Basic ' + org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_password).bytes)
        ]

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "request body sent from repose to the origin service should contain"
        mc.receivedResponse.code as Integer == filterStatusCode
        mc.handlings.size() == 0

        where:
        reqTenant | identityStatusCode       | filterStatusCode
        9400      | SC_BAD_REQUEST           | SC_UNAUTHORIZED
        9401      | SC_UNAUTHORIZED          | SC_UNAUTHORIZED
        9403      | SC_FORBIDDEN             | SC_FORBIDDEN
        9404      | SC_NOT_FOUND             | SC_UNAUTHORIZED
        9500      | SC_INTERNAL_SERVER_ERROR | SC_INTERNAL_SERVER_ERROR
        9501      | SC_NOT_IMPLEMENTED       | SC_INTERNAL_SERVER_ERROR
        9502      | SC_BAD_GATEWAY           | SC_INTERNAL_SERVER_ERROR
        9503      | SC_SERVICE_UNAVAILABLE   | SC_INTERNAL_SERVER_ERROR
        9504      | SC_GATEWAY_TIMEOUT       | SC_INTERNAL_SERVER_ERROR
    }

    def "When the request does have an x-auth-token, then still work with client-auth"() {
        given: "the X-Auth-Token authentication header containing the User Token"
        def headers = ['X-Auth-Token': fakeIdentityService.client_token]

        when: "the request already has an x-auth-token header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get a token and validate it"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.getFirstValue(AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(WWW_AUTHENTICATE)
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token")
        //**This test tracing header Repose handling request, and the request to identity as part of REP-1704**
        mc.orphanedHandlings.each {
            e ->
                assert e.request.headers.contains("x-trans-id")
                assert e.request.headers.getFirstValue("x-trans-id") == mc.handlings[0].request.headers.getFirstValue("x-trans-id")
        }
    }
}
