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
 * Created by jennyvo on 9/11/15.
 * if pre-authorized-roles if present repose will bypass valkyrie authorization
 * note: using keystone-v2 auth filter
 */
class CullingBypassValkyrieAuthorizationTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static valkyrieEndpoint

    def static MockIdentityV2Service fakeIdentityV2Service
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
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/bypassvalkyrie", params);
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/bypassvalkyrie/withculling", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
        fakeIdentityV2Service.checkTokenValid = true

        fakeValkyrie = new MockValkyrie(properties.valkyriePort)
        valkyrieEndpoint = deproxy.addEndpoint(properties.valkyriePort, 'valkyrie service', null, fakeValkyrie.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    @Unroll("permission: #permission for #method with tenant: #tenantID and deviceIDs: #deviceID, #deviceID2 should return a #responseCode")
    def "Test get match resource list"() {
        given: "a user defined in Identity"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = tenantID
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            device_id = deviceID
            device_id2 = deviceID2
            device_perm = permission
        }

        and: "a JSON Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method,
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
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

    def "Test missing tenantid"() {
        given: "a user defined in Identity"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = ""
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            device_id = "520707"
            device_id2 = "520708"
            device_perm = "view_product"
        }

        and: "a JSON Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                        'x-contact-id': '123456'
                ],
                defaultHandler: jsonResp
        )

        then:
        mc.receivedResponse.code == "401"
    }

    // pre-authorized-roles have been config should be bypassed valkyrie authorization
    // Just a pass through but not actual talk nor get any response from valkyrie
    def "Bypass Valkyrie authorization on Culling"() {
        given: "a list permission devices defined in Valkyrie token without tenantid associated with"
        fakeIdentityV2Service.with {
            client_token = "rackerSSO"
            service_admin_role = "racker"
            client_userid = "rackerSSOUsername"
        }

        and: "permissions defined in Valkyrie"
        fakeValkyrie.with {
            device_id = "520707"
            device_id2 = "520708"
            device_perm = "view_product"
        }

        and: "a JSON Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], jsonrespbody) }

        when: "a request is made against a device with Valkyrie set permissions"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: "GET",
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityV2Service.client_token,
                ],
                defaultHandler: jsonResp
        )

        then: "check response"
        mc.receivedResponse.code == "200"
        // verify not interact with valkyrie
        mc.orphanedHandlings.each {
            e -> assert !e.request.path.contains("/resources")
        }


    }

    def String randomTenant() {
        "hybrid:" + random.nextInt()
    }
}
