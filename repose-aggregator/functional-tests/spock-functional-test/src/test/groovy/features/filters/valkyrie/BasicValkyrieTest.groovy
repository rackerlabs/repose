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

import static javax.servlet.http.HttpServletResponse.SC_OK

class BasicValkyrieTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie
    Map params = [:]

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);

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


    def "Test fine grain access of resources based on Valkyrie permissions (no rbac)"() {
        given: "A device ID with a particular permission level defined in Valykrie"

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        "x-roles": "raxRolesDisabled",
                        "X-Device-Id": deviceID     /* remove this once we have the api-validator piece */
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode


        where:
        method      |   tenantID    |   deviceID    | permission        | responseCode
        "GET"       |   "12345"     |   "520707"    | "view_product"    | "200"
        "HEAD"      |   "12345"     |   "520707"    | "view_product"    | "200"
        "PUT"       |   "12345"     |   "520707"    | "view_product"    | "403"
        "POST"      |   "12345"     |   "520707"    | "view_product"    | "403"
        "DELETE"    |   "12345"     |   "520707"    | "view_product"    | "403"
        "PATCH"     |   "12345"     |   "520707"    | "view_product"    | "403"
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


    /*
    def "Test fine grain access of resources based on Valkyrie permissions (rbac enabled)"() {
        given: "A device ID with a particular permission level defined in Valykrie"

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        when: "a #method is made against a device that has a permission of #permission"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        "x-roles": "raxRolesEnabled, a:observer",
                        "X-Device-Id": deviceID  //remove once we have the api-validator piece
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode


        where:
        method      |   tenantID    |   deviceID    | permission        | responseCode
        "GET"       |   "12345"     |   "520707"    | "view_product"    | "200"
        "HEAD"      |   "12345"     |   "520707"    | "view_product"    | "200"
        "PUT"       |   "12345"     |   "520707"    | "view_product"    | "403"
        "POST"      |   "12345"     |   "520707"    | "view_product"    | "403"
        "DELETE"    |   "12345"     |   "520707"    | "view_product"    | "403"
        "PATCH"     |   "12345"     |   "520707"    | "view_product"    | "403"
        "GET"       |   "12345"     |   "520707"    | "admin_product"   | "200"
        "HEAD"      |   "12345"     |   "520707"    | "admin_product"   | "200"
        "PUT"       |   "12345"     |   "520707"    | "admin_product"   | "403"
        "POST"      |   "12345"     |   "520707"    | "admin_product"   | "403"
        "DELETE"    |   "12345"     |   "520707"    | "admin_product"   | "403"
        "GET"       |   "12345"     |   "520707"    | "edit_product"    | "200"
        "HEAD"      |   "12345"     |   "520707"    | "edit_product"    | "200"
        "PUT"       |   "12345"     |   "520707"    | "edit_product"    | "403"
        "POST"      |   "12345"     |   "520707"    | "edit_product"    | "403"
        "DELETE"    |   "12345"     |   "520707"    | "edit_product"    | "403"
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
*/

    def "Test valkyrie filter delegable mode."() {
        given: "a configuration change where valkyrie filter delegates error messaging"

        repose.configurationProvider.applyConfigs("features/filters/valkyrie/delegable", params);
        sleep 15000

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
        }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        "x-roles": "raxRolesDisabled",
                        "X-Device-Id": deviceID     /* remove this once we have the api-validator piece */
                ]
        )

        then: "origin service should be forworded errors from valkyrie filter in header"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains(delegatedMsg)
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains("q=0.7")

        where:
        method      |   tenantID    |   deviceID    | permission        | delegatedMsg
        "PUT"       |   "12345"     |   "520707"    | "view_product"    | "status_code=403"
        "POST"      |   "12345"     |   "520707"    | "view_product"    | "status_code=403"
        "DELETE"    |   "12345"     |   "520707"    | "view_product"    | "status_code=403"
        "PATCH"     |   "12345"     |   "520707"    | "view_product"    | "status_code=403"
        "GET"       |   "12345"     |   "520707"    | ""                | "status_code=403"
        "HEAD"      |   "12345"     |   "520707"    | ""                | "status_code=403"
        "PUT"       |   "12345"     |   "520707"    | ""                | "status_code=403"
        "POST"      |   "12345"     |   "520707"    | ""                | "status_code=403"
        "DELETE"    |   "12345"     |   "520707"    | ""                | "status_code=403"
        "GET"       |   "12345"     |   "520707"    | "shazbot_prod"    | "status_code=403"
        "HEAD"      |   "12345"     |   "520707"    | "prombol"         | "status_code=403"
        "PUT"       |   "12345"     |   "520707"    | "hezmol"          | "status_code=403"
        "POST"      |   "12345"     |   "520707"    | "_22_reimer"      | "status_code=403"
        "DELETE"    |   "12345"     |   "520707"    | "blah"            | "status_code=403"
    }

}
