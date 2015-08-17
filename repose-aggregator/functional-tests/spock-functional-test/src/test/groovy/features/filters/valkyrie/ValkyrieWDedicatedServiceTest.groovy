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
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/10/15.
 */
class ValkyrieWDedicatedServiceTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityService fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]
    def static deviceId1 = "520707"
    def static deviceId2 = "520708"

    def static random = new Random()
    def static String jsonrespbody = "{\n" +
            "    \"values\": [\n" +
            "        {\n" +
            "            \"id\": \"en6bShuX7a\",\n" +
            "            \"label\": \"brad@morgabra.com\",\n" +
            "            \"ip_addresses\": null,\n" +
            "            \"metadata\": {\n" +
            "                \"userId\": \"325742\",\n" +
            "                \"email\": \"brad@morgabra.com\"\n" +
            "            },\n" +
            "            \"managed\": false,\n" +
            "            \"uri\": \"http://butts.com/device/$deviceId1\",\n" +
            "            \"agent_id\": \"e333a7d9-6f98-43ea-aed3-52bd06ab929f\",\n" +
            "            \"active_suppressions\": [],\n" +
            "            \"scheduled_suppressions\": [],\n" +
            "            \"created_at\": 1405963090100,\n" +
            "            \"updated_at\": 1409247144717\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"enADqSly1y\",\n" +
            "            \"label\": \"test\",\n" +
            "            \"ip_addresses\": null,\n" +
            "            \"metadata\": null,\n" +
            "            \"managed\": false,\n" +
            "            \"uri\": \"http://butts.com/device/$deviceId2\",\n" +
            "            \"agent_id\": null,\n" +
            "            \"active_suppressions\": [],\n" +
            "            \"scheduled_suppressions\": [],\n" +
            "            \"created_at\": 1411055897191,\n" +
            "            \"updated_at\": 1411055897191\n" +
            "        }\n" +
            "    ],\n" +
            "    \"metadata\": {\n" +
            "        \"count\": 2,\n" +
            "        \"limit\": 2,\n" +
            "        \"marker\": null,\n" +
            "        \"next_marker\": \"enB11JvqNv\",\n" +
            "        \"next_href\": \"https://monitoring.api.rackspacecloud.com/v1.0/731078/entities?limit=2&marker=enB11JvqNv\"\n" +
            "    }\n" +
            "}"


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
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceIDs: #deviceID, #deviceID2 should return a #responseCode")
    def "Test fine grain access of resources based on Valkyrie permissions (no rbac)"() {
        given: "A device ID with a particular permission level defined in Valkyrie"
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
            client_tenant = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                ],
                defaultHandler: jsonResp
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }


        where:
        method | tenantID       | deviceID  | deviceID2  | permission       | responseCode
        "GET"  | randomTenant() | "520707"  | "511123"   | "view_product"   | "200"
        "HEAD" | randomTenant() | "520707"  | "511124"   | "view_product"   | "200"
        "GET"  | randomTenant() | "520707"  | "511123"   | "admin_product"  | "200"
    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
