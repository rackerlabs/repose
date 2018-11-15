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

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Slow
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/13/15.
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */
@Category(Slow)
class CollectResourceBaseOnPermissionTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityService
    def static MockValkyrie fakeValkyrie
    def static Map params = [:]
    def static deviceId1 = "520707"
    def static deviceId2 = "520708"

    def static random = new Random()
    def static String jsonrespbody = """{
        "values": [
            {
                "id": "en6bShuX7a",
                "label": "brad@morgabra.com",
                "ip_addresses": null,
                "metadata": {
                    "userId": "325742",
                    "email": "brad@morgabra.com"
                },
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/123456/devices/$deviceId1",
                "agent_id": "e333a7d9-6f98-43ea-aed3-52bd06ab929f",
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1405963090100,
                "updated_at": 1409247144717
            },
            {
                "id": "enADqSly1y",
                "label": "test",
                "ip_addresses": null,
                "metadata": null,
                "managed": false,
                "uri": "http://core.rackspace.com/accounts/123456/devices/$deviceId2",
                "agent_id": null,
                "active_suppressions": [],
                "scheduled_suppressions": [],
                "created_at": 1411055897191,
                "updated_at": 1411055897191
            }
        ],
        "metadata": {
            "count": 2,
            "limit": 2,
            "marker": null,
            "next_marker": "enB11JvqNv",
            "next_href": "https://monitoring.api.rackspacecloud.com/v1.0/731078/entities?limit=2&marker=enB11JvqNv"
        }
    }"""

    def setupSpec() {
        deproxy = new Deproxy()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/collectionresources", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceIDs: #deviceID, #deviceID2 should return a #responseCode")
    def "Test get match resource list"() {
        given: "a list permission devices defined in Valkyrie"
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )
        def body = new String(mc.receivedResponse.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == responseCode
        result.values.size == size
        result.metadata.count == size

        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")
        //**This part for tracing header test REP-1704**
        // any requests send to identity also include tracing header
        mc.orphanedHandlings.each {
            e -> assert e.request.headers.contains("x-trans-id")
        }

        where:
        method | tenantID       | deviceID | deviceID2 | permission     | responseCode | size
        "GET"  | randomTenant() | "520707" | "511123"  | "view_product" | "200"        | 1
        "GET"  | randomTenant() | "520708" | "511123"  | "view_product" | "200"        | 1
        "GET"  | randomTenant() | "520707" | "520708"  | "view_product" | "200"        | 2
        "GET"  | randomTenant() | "520705" | "520706"  | "view_product" | "200"        | 0
    }

    @Unroll("account_admin with device id permission: #deviceid1, #deviceid2, response list no. item #size")
    def "enable-bypass-account-admin false, account_admin only get device within its permission"() {
        given: "a list permission devices defined in Valkyrie"
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_perm = "account_admin"
            device_id = deviceid1
            device_id2 = deviceid2
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )
        def body = new String(mc.receivedResponse.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        result.values.size == size
        result.metadata.count == size

        where:
        deviceid1 | deviceid2 | size
        "520707"  | "520708"  | 2
        "520708"  | "520707"  | 2
        "520713"  | "520707"  | 1
        "520708"  | "520711"  | 1
        "520712"  | "520711"  | 0

    }

    @Unroll("Origin RespCode: #originResp")
    def "Verify no culling on non 2xx responses"() {
        given: "a list permission devices defined in Valkyrie"
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(originResp, "OK", ["content-type": "application/json"], null) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == originResp

        where:
        method | tenantID       | deviceID | deviceID2 | permission     | originResp | size
        "GET"  | randomTenant() | "520707" | "511123"  | "view_product" | "400"      | 0
        "GET"  | randomTenant() | "520708" | "511123"  | "view_product" | "401"      | 0
        "GET"  | randomTenant() | "520707" | "520708"  | "view_product" | "403"      | 0
        "GET"  | randomTenant() | "520705" | "520706"  | "view_product" | "500"      | 0
        "GET"  | randomTenant() | "520705" | "520706"  | "view_product" | "502"      | 0
    }

    @Unroll("Origin RespCode: #originResp")
    def "Case 2xx response with empty body"() {
        given: "a list permission devices defined in Valkyrie"
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(originResp, "OK", ["content-type": "application/json"], null) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "502"

        where:
        method | tenantID       | deviceID | deviceID2 | permission     | originResp | size
        "GET"  | randomTenant() | "520707" | "511123"  | "view_product" | "200"      | 0
        "GET"  | randomTenant() | "520708" | "511123"  | "view_product" | "204"      | 0
    }

    @Unroll('#charset encoded response should be culled')
    def 'encoded response should be culled'() {
        given:
        String tenantId = randomTenant()

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantId
        }

        fakeValkyrie.with {
            device_id = deviceId1
            device_id2 = deviceId2
            device_perm = 'view_product'
        }

        byte[] responseBody = '''
        {
            "values": [
                {
                    "id": "en6bShuX7a",
                    "label": "test\u00A0nbsp",
                    "uri": "http://core.rackspace.com/accounts/123456/devices/520705"
                }
            ],
            "metadata": {
                "count": 1
            }
        }'''.getBytes(charset)
        Closure<Response> responseClosure = { request ->
            new Response(200, null, ['content-type': "application/json; charset=$charset"], responseBody)
        }

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + '/resources', method: 'GET',
                headers: [
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'],
                defaultHandler: responseClosure)
        Map result = new JsonSlurper().parseText(new String(mc.receivedResponse.body as byte[], charset)) as Map

        then:
        mc.receivedResponse.code.toInteger() == 200
        result.values.size == 0
        result.metadata.count == 0

        where:
        charset << ['UTF-8', 'UTF-16', 'ISO-8859-1', 'ASCII']
    }

    @Unroll('#charset encoded response should NOT be culled')
    def 'encoded response should NOT be culled'() {
        given: "a list permission devices defined in Valkyrie"
        def tenantID = randomTenant()
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        fakeValkyrie.with {
            device_perm = "account_admin"
            device_id = "520713"
            device_id2 = "520707"
        }

        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json; charset=$charset"], jsonrespbody.getBytes(charset)) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )
        // Handle the cases (e.g. UTF-16) where Deproxy doesn't automagically turn the received response body byte[] into a String.
        def body = (mc.receivedResponse.body instanceof String) ? mc.receivedResponse.body : new String(mc.receivedResponse.body, charset)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then: "check response"
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
        result.values.size == 1
        result.metadata.count == 1

        where:
        charset << ['UTF-8', 'UTF-16', 'ISO-8859-1', 'ASCII']
    }

    def String randomTenant() {
        'hybrid:' + random.nextInt()
    }
}
