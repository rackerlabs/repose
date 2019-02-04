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
package features.filters.keystonev2basicauth.timeout

import org.apache.commons.codec.binary.Base64
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Identity

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

/**
 * Created by jennyvo on 11/9/15.
 * using default pool with socket and http time out with 20 sec
 * Update on 01/21/16
 *  - Replace client-auth-n with keystone-v2 filter
 */
@Category(Identity)
class BasicAuthTimeoutUsingConnPoolTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/keystonev2basicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/keystonev2basicauth/timeout", params);

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

    def "timeout test, auth response time out is less than socket connection time out"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]
        "Delay 19 sec"
        fakeIdentityService.with {
            sleeptime = 19000
        }

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "Request should be passed from repose"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_OK
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        // REP-3577 - add header using connection pool
        mc.orphanedHandlings.get(0).request.headers.contains("auth-proxy")
        mc.orphanedHandlings.get(0).request.headers.getFirstValue("auth-proxy") == "testing"
        !mc.handlings[0].request.headers.contains("auth-proxy")
    }

    def "timeout test, auth response time out is greater than socket connection time out"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]
        "Delay 19 sec"
        fakeIdentityService.with {
            sleeptime = 22000
        }

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "Request should not be passed from repose"
        mc.receivedResponse.code == "500"//HttpServletResponse.SC_GATEWAY_TIMEOUT
        mc.handlings.size() == 0
        sleep(1000)
        reposeLogSearch.searchByString("I/O error: Read timed out").size() > 0
        reposeLogSearch.searchByString("NullPointerException").size() == 0
    }
}
