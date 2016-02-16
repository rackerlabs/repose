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
package features.filters.identitybasicauth

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.apache.commons.codec.binary.Base64
import org.openrepose.commons.utils.http.HttpDate
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.springframework.http.HttpHeaders
import spock.lang.Ignore
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static org.openrepose.core.filter.logic.FilterDirector.SC_TOO_MANY_REQUESTS

/**
 * Created by jennyvo on 9/17/14.
 * Basic Auth filter can't be used alone, have to use with client-auth filter
 */
class BasicAuthStandaloneTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV2Service fakeIdentityService
    ReposeLogSearch reposeLogSearch

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth/onlybasicauth", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true
    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
        }
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile())
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when start repose with basic auth, send request without credential"() {
        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
    }

    // NOTE: This would normally be removed by a Header Normalization filter.
    def "Request that contains both an X-Auth-Token and HTTP Basic authentication header is sent."() {
        given: "header containing the User Token and an HTTP Basic authentication header (username/apikey)"
        def headers = [
                "X-Auth-Token"             : fakeIdentityService.client_token,
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request already has credentials"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "simply pass it on down the filter chain NOT processing the HTTP Basic authentication"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        !mc.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    @Unroll("Sending request with invalid UserName #userName and API Key #apiKey pair.")
    def "Fail to retrieve a token for an HTTP Basic authentication header with an invalid UserName/ApiKey pair"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((userName + ":" + apiKey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "Request reject if invalid apikey or username"
        mc.receivedResponse.code == SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")

        where:
        userName                            | apiKey
        fakeIdentityService.client_username | "BAD-API-KEY"
        "BAD-USER-NAME"                     | fakeIdentityService.client_apikey
        "BAD-USER-NAME"                     | "BAD-API-KEY"
        ""                                  | "BAD-AIP-KEY"
        "BAD-USER-NAME"                     | ""
        ""                                  | fakeIdentityService.client_apikey
        fakeIdentityService.client_username | ""
        ""                                  | ""
    }

    def "Fail to retrieve a token for an HTTP Basic authentication wiht only header and 'Basic' keyword"() {
        given: "the Authorization with 'Basic' keyword"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic '
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "Request reject if invalid apikey or username"
        mc.receivedResponse.code == SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    @Unroll("Case: #authorizationvalue")
    def "Test additional cases passing through with basic auth filter"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): authorizationvalue
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "Request reject if invalid apikey or username"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        //mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")

        where:
        authorizationvalue <<
                [Base64.encodeBase64URLSafeString((":testkey").bytes),
                 Base64.encodeBase64URLSafeString(("testuser:").bytes),
                 Base64.encodeBase64URLSafeString((":").bytes),
                 "something " + Base64.encodeBase64URLSafeString(("testuser:testkey").bytes),
                 "something " + Base64.encodeBase64URLSafeString((":").bytes)]
    }

    // Only the first AUTHORIZATION Basic header will be processed.
    def "Stop trying to retrieve a token for an HTTP Basic authentication header after a token has been obtained."() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + "BAD-API-KEY").bytes),
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes),
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString(("BAD-USER-NAME" + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token for it"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
    }

    // NOTE: This would normally be removed by a Header Normalization filter.
    def "when start repose with basic auth only, x-auth-token should work"() {
        given:
        def headers = [
                "X-Auth-Token": fakeIdentityService.client_token
        ]

        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        !mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    @Ignore
    @Unroll("Test username: #username and api key: #password")
    def "Retrieve a token for an HTTP Basic authentication header with UserName/ApiKey"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_apikey = password
            client_token = UUID.randomUUID().toString()
        }
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((username + ":" + password).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token for it"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(HttpHeaders.AUTHORIZATION) == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc.orphanedHandlings.size() == 1 // This is the call to the Mock Identity service through deproxy.
        !mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")

        where:
        username                            | password
        fakeIdentityService.client_username | UUID.randomUUID().toString()
    }

    def "Inject header WWW-authenticate when basicauth or other component failed with 401"() {
        when: "the request sends with invalid key"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET',
                defaultHandler: { new Response(SC_UNAUTHORIZED, null, null, null) })

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "Log a very loud WARNING stating the OpenStack Basic Auth filter cannot be used alone."() {
        expect: "check for the WARNING."
        reposeLogSearch.searchByString("WARNING: This filter cannot be used alone, it requires an AuthFilter after it.").size() > 0
    }

    @Unroll("Sending request to the Identity service returns HTTP Status Code #identityStatusCode")
    def "when failing to authenticate admin client"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key and the Mock Identity Service's generateTokenHandler"
        fakeIdentityService.with {
            generateTokenHandler = {
                request, xml ->
                    new Response(identityStatusCode, null, null, null)
            }
        }
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == filterStatusCode.toString()
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

    @Unroll("Sending request with auth admin response set to HTTP #identityStatusCode and a Retry-After header")
    def "when failing to authenticate admin client with temporary failure"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key and the Mock Identity Service's generateTokenHandler"
        def retryCalendar = new GregorianCalendar()
        retryCalendar.add(Calendar.MINUTE, 5)
        def retryString = new HttpDate(retryCalendar.getTime()).toRFC1123()
        fakeIdentityService.with {
            generateTokenHandler = {
                request, xml ->
                    new Response(identityStatusCode, null, [(HttpHeaders.RETRY_AFTER): retryString], xml)
            }
        }
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == SC_SERVICE_UNAVAILABLE.toString()
        mc.receivedResponse.getHeaders().getFirstValue(HttpHeaders.RETRY_AFTER).equals(retryString)
        reposeLogSearch.searchByString("Missing ${HttpHeaders.RETRY_AFTER} header on Auth Response status code: $identityStatusCode").size() == 0

        where:
        reqTenant | identityStatusCode
        9505      | SC_REQUEST_ENTITY_TOO_LARGE
        9506      | SC_TOO_MANY_REQUESTS
    }
}
