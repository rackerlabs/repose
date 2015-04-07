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
package features.filters.valkyrie

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import framework.mocks.MockValkyrie
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

class BasicValkyrieTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {

        // Initialize state of the mock identity

        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
        }

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Retrieve a token for an HTTP Basic authentication header with UserName/ApiKey"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeValkyrie.client_username + ":" + fakeValkyrie.client_apikey).bytes)
        ]

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        //String sValkyrieEndpoint = "http://${properties.targetHostname}:${properties.valkyriePort}"
        //MessageChain mc = deproxy.makeRequest(url: sValkyrieEndpoint, method: 'GET', headers: headers)
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "check response"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstVal ue("X-Auth-Token").equals(fakeValkyrie.client_token)
        mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)



        where:
        method      |   tenantID    |   deviceID    | permission        | responseCode
        "GET"       |   "12345"     |   "520707"    | "view_product"    | "200"
        "HEAD"      |   "12345"     |   "520708"    | "view_product"    | "200"
        "PUT"       |   "12345"     |   "520707"    | "view_product"    | "403"
        "POST"      |   "12345"     |   "520707"    | "view_product"    | "403"
        "DELETE"    |   "12345"     |   "520707"    | "view_product"    | "403"
        "GET"       |   "12345"     |   "520707"    | "admin_product"   | "200"
        "HEAD"      |   "12345"     |   "520707"    | "admin_product"   | "200"
        "PUT"       |   "12345"     |   "520707"    | "admin_product"   | "200"
        "POST"      |   "12345"     |   "520707"    | "admin_product"   | "200"
        "DELETE"    |   "12345"     |   "520707"    | "admin_product"   | "200"
        "GET"       |   "12345"     |   "520707"    | "edit_product"    | "200"
        "HEAD"      |   "12345"     |   "520707"    | "edit_product"    | "200"
        "PUT"       |   "12345"     |   "520707"    | "edit_product"    | "200"
        "POST"      |   "12345"     |   "520707"    | "edit_product"    | "200"
        "DELETE"    |   "12345"     |   "520707"    | "edit_product"    | "200"
        "GET"       |   "12345"     |   "520707"    | ""                | "403"
        "HEAD"      |   "12345"     |   "520707"    | ""                | "403"
        "PUT"       |   "12345"     |   "520707"    | ""                | "403"
        "POST"      |   "12345"     |   "520707"    | ""                | "403"
        "DELETE"    |   "12345"     |   "520707"    | ""                | "403"
        "GET"       |   "12345"     |   "520707"    | "shazbot_prod"    | "403"
        "HEAD"      |   "12345"     |   "520707"    | "prombol"         | "403"
        "PUT"       |   "12345"     |   "520707"    | "hezmol"          | "403"
        "POST"      |   "12345"     |   "520707"    | "_22_reimer"      | "403"
        "DELETE"    |   "12345"     |   "520707"    | "blah"            | "403"

    }

}
