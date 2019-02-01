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
import org.openrepose.framework.test.mocks.MockValkyrie
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Shared

@Category(Filters)
class PassNonDedicatedTenantTest extends ReposeValveTest {

    @Shared
    MockValkyrie mockValkyrie

    @Shared
    String deviceId1 = "520707"
    @Shared
    String deviceId2 = "520708"

    @Shared
    String responseBody = """{
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
                "scheduled_suppressions": []
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
                "scheduled_suppressions": []
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
        Map params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/valkyrie/passnondedicatedtenant", params)

        repose.start()

        mockValkyrie = new MockValkyrie(properties.valkyriePort)

        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.targetPort, name: "origin service", defaultHandler: {
            new Response(200, "OK", ["Content-Type": "application/json", "Content-Length": responseBody.length()], responseBody)
        })
        deproxy.addEndpoint(port: properties.valkyriePort, name: "valkyrie service", defaultHandler: mockValkyrie.getHandler())
    }

    def setup() {
        mockValkyrie.resetCounts()
        mockValkyrie.resetHandlers()
        mockValkyrie.resetParameters()
    }

    def "when handling a request for a non-dedicated tenant's resource, the request should be passed without modification"() {
        given:
        String tenantId = "12345"

        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/resources/foo",
                method: "GET",
                headers: ["X-Tenant-Id": tenantId])

        then:
        mockValkyrie.getAuthorizationCount() == 0
        mc.orphanedHandlings.size() == 0
        mc.handlings.size() == 1
        mc.handlings[0].request.path == "/resources/foo"
        mc.handlings[0].request.headers.contains("X-Tenant-Id")
        mc.handlings[0].request.headers.getFirstValue("X-Tenant-Id") == tenantId
        mc.receivedResponse.code.toInteger() == 200
        mc.receivedResponse.body == responseBody
    }

    def "when handling a request for a dedicated tenant's resource, authorization and culling are performed"() {
        given:
        String tenantId = "hybrid:12345"
        String contactId = "20583"
        String permission = "view_product"

        mockValkyrie.with {
            device_id = deviceId1
            device_perm = permission
        }

        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/resources/foo",
                method: "GET",
                headers: ["X-Tenant-Id" : tenantId,
                          "X-Contact-Id": contactId,
                          "X-Device-Id" : deviceId1])
        def body = new String(mc.receivedResponse.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then:
        mockValkyrie.getAuthorizationCount() == 1
        mc.orphanedHandlings.size() == 1
        mc.handlings.size() == 1
        mc.receivedResponse.code.toInteger() == 200
        result.values.size == 1
        result.metadata.count == 1
    }
}
