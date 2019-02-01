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
package features.filters.keystonev2.authorization


import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.string.Base64Helper.base64DecodeUtf8

@Category(Filters)
class AuthorizationHeadersTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/authorization/headers", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(
            properties.identityPort,
            'identity service',
            null,
            fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.client_tenantid = UUID.randomUUID().toString()
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        fakeIdentityV2Service.originServicePort = properties.targetPort
        reposeLogSearch.cleanLog()
    }

    def "The authentication should add the headers for authorization."() {
        when: "User sends a request through Repose"
        MessageChain mc = deproxy.makeRequest(
            url: """$reposeEndpoint/ss""",
            method: "GET",
            headers: ["X-Auth-Token": fakeIdentityV2Service.client_token])

        then: "User should receive a 200 response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "It should have never made it to the origin service"
        mc.handlings.size() == 1

        and: "It should have an encoded X-Map-Roles header"
        def mapRolesRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Map-Roles")
        mapRolesRequestHeaders.size() == 1
        def mapRolesDecode = base64DecodeUtf8(mapRolesRequestHeaders[0]).split("[\\[\\]{}:,\"]") as Set
        if (mapRolesDecode.contains("")) {
            mapRolesDecode.remove("")
        }
        mapRolesDecode == [fakeIdentityV2Service.client_tenantid, "compute", "admin",
                           "repose/domain/roles", "service", "admin-role1",
                           "this-is-the-nast-id", "object-store", "admin"] as Set

        and: "It should have an encoded X-Catalog header"
        def catalogRequestHeaders = mc.handlings[0].request.getHeaders().findAll("X-Catalog")
        catalogRequestHeaders.size() == 1
        def catalogDecode = base64DecodeUtf8(catalogRequestHeaders[0])
        catalogDecode.contains("/tokens/${fakeIdentityV2Service.client_token}/endpoints?'marker=5&limit=10'")
        catalogDecode.contains(""""internalURL": "http://localhost:${fakeIdentityV2Service.originServicePort}/",""")
        catalogDecode.contains(""""name": "swift",""")
        catalogDecode.contains(""""name": "nova_compat",""")
        catalogDecode.contains(""""name": "OpenStackService",""")
        catalogDecode.contains(""""adminURL": "http://localhost:${fakeIdentityV2Service.originServicePort}/",""")
        catalogDecode.contains(""""region": "ORD",""")
        catalogDecode.contains(""""tenantId": "${fakeIdentityV2Service.client_tenantid}",""")
        catalogDecode.contains(""""type": "object-store",""")
        catalogDecode.contains(""""type": "compute",""")
        catalogDecode.contains(""""type": "service",""")
        catalogDecode.contains(""""id": 1,""")
        catalogDecode.contains(""""id": 2,""")
        catalogDecode.contains(""""id": 3,""")
        catalogDecode.contains(""""publicURL": "http://localhost:${fakeIdentityV2Service.originServicePort}/""")
    }
}
