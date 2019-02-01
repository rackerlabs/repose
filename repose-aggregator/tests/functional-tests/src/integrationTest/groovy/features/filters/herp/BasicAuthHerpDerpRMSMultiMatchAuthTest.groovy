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
package features.filters.herp

import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.RandomStringUtils
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Ignore

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

/**
 * Created by jennyvo on 10/7/15.
 * Update on 01/21/16
 *  - Replace client-auth-n with keystone-v2 filter
 */
@Ignore
@Category(Filters)
class BasicAuthHerpDerpRMSMultiMatchAuthTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatormultimatch", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatormultimatch/wauthpreference", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatormultimatch/wauthpreference/wbasicauth", params)

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
        }
    }

    /*
    These tests are to verify the delegation of basicauth failures to the derp filter, which then forwards
    that information back to the client.  The origin service, thus, never gets invoked.
    */

    def "When the request send with invalid long key or username, then will fail to authenticate"() {
        given: "the HTTP Basic authentication header containing the User Name and invalid API Key"
        def key = RandomStringUtils.random(226, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + key).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get a token and validate it"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_UNAUTHORIZED
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "When req with invalid key using delegable mode with quality"() {
        given:
        def key = RandomStringUtils.random(99, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + key).bytes)
        ]

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1234",
                method: 'GET',
                headers: headers)

        then:
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.contains("content-type")
        mc.receivedResponse.body.contains("Failed to authenticate user: " + fakeIdentityService.client_username)
        mc.handlings.size() == 0
        mc.getOrphanedHandlings().size() == 1
    }

    // identity currently return 400 bad request for api-key > 100 characters
    // repose log REP-2880 to work on compliant with this response
    def "When req with invalid key > 100 char using delegable mode with quality"() {
        given:
        def key = RandomStringUtils.random(120, 'ABCDEFGHIJKLMNOPQRSTUVWYZabcdefghijklmnopqrstuvwyz-_1234567890')
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + key).bytes)
        ]

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1234",
                method: 'GET',
                headers: headers)

        then:
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.contains("content-type")
        // may expect different message here
        mc.receivedResponse.body.contains("Bad Request received from identity service for " + fakeIdentityService.client_username)
        mc.handlings.size() == 0
        mc.getOrphanedHandlings().size() == 1
    }
}
