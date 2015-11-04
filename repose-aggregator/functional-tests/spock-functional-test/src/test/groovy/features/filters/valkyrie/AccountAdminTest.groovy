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
import framework.category.Slow
import framework.mocks.MockIdentityService
import framework.mocks.MockValkyrie
import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Updated by jennyvo on 11/04/15.
 * if account_admin role make additional call (inventory) to valkyrie to get the full list of devices
 *  Test with:
 *      mock valkyrie return with a list of > 500 devices
 *      mock origin services response return > 5000 devices
 */
@Category(Slow)
class AccountAdminTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]

    def static random = new Random()

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/accountadmin", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
        fakeIdentityService.resetHandlers()
        fakeIdentityService.resetDefaultParameters()
        fakeValkyrie.resetHandlers()
        fakeValkyrie.resetParameters()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll("#method device #deviceID with permission #permission, tenant: #tenantID should return a #responseCode")
    def "#method device #deviceID with permission #permission, tenant: #tenantID should return a #responseCode"() {
        given: "A "
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_perm = permission
            multiplier = 500
        }

        when: "a #method request is made to access device #deviceID"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ]
        )

        def accountid = fakeValkyrie.tenant_id
        def contactid = fakeIdentityService.contact_id

        then: "the response should be #responseCode and #permission should be in the Requests the X-Roles header"
        mc.receivedResponse.code == responseCode
        // user device permission translate to roles
        mc.getHandlings().get(0).getRequest().headers.findAll("x-roles").contains(permission)
        mc.getHandlings().get(0).getRequest().headers.getFirstValue("x-device-id") == deviceID
        // orphanedhandlings should include the original call + account inventoty call
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/permissions/contacts/any/by_contact/" + contactid + "/effective")
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/inventory")

        where:
        method   | tenantID       | deviceID | permission      | responseCode
        "HEAD"   | randomTenant() | "520707" | "account_admin" | "200"
        "GET"    | randomTenant() | "520707" | "account_admin" | "200"
        "PUT"    | randomTenant() | "520707" | "account_admin" | "200"
        "POST"   | randomTenant() | "520707" | "account_admin" | "200"
        "PATCH"  | randomTenant() | "520707" | "account_admin" | "200"
        "DELETE" | randomTenant() | "520707" | "account_admin" | "200"
    }

    def "Test get match resource list with large list 5000 devices"() {
        given: "a list permission devices defined in Valkyrie"
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantID
        }

        fakeValkyrie.with {
            account_perm = "account_admin"
            multiplier = 500
        }

        def jsonbody = genJsonResp(5000)

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/9999", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456',
                        'x-tenant-id' : tenantID
                ],
                defaultHandler: jsonResp
        )
        def accountid = fakeValkyrie.tenant_id
        def contactid = fakeIdentityService.contact_id
        def body = new String(mc.receivedResponse.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        result.values.size == 5001
        result.metadata.count == 5001
        // orphanedhandlings should include the original call + account inventoty call
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/permissions/contacts/any/by_contact/" + contactid + "/effective")
        mc.orphanedHandlings.request.path.toString().contains("/account/" + accountid + "/inventory")
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }

    def String randomDevice() {
        def listdevices = ["520707", "520708", "520709", "520710", "520711", "520712", "520713"]
        return listdevices.get(random.nextInt(listdevices.size()))
    }

    def String genJsonResp(def number) {
        def value = number + 1
        String meat = """{
        "values": ["""
        1.upto(number) {
            meat += """{
                "id": "en6bShuX7a",
                "label": "brad@morgabra.com",
                "ip_addresses": null,
                "metadata": {
                    "userId": "325742",
                    "email": "brad@morgabra.com"
                },
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/123456/devices/""" + randomDevice() + """",
                "agent_id": "e333a7d9-6f98-43ea-aed3-52bd06ab929f",
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1405963090100,
                "updated_at": 1409247144717
            },"""
        }
        meat += """{
                "id": "enADqSly1y",
                "label": "test",
                "ip_addresses": null,
                "metadata": null,
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/123456/devices/""" + randomDevice() + """",
                "agent_id": null,
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1411055897191,
                "updated_at": 1411055897191
            }],
            "metadata": {
                "count": $value,
                "limit": $value,
                "marker": null,
                "next_marker": "enB11JvqNv",
                "next_href": "https://monitoring.api.rackspacecloud.com/v1.0/731078/entities?limit=2&marker=enB11JvqNv"
            }
        }"""
    }
}
