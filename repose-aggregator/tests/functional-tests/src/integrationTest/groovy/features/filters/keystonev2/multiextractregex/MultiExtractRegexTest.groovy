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
package features.filters.keystonev2.multiextractregex

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Identity
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

@Category(Identity)
class MultiExtractRegexTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/multiextractregex", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
    }

    def setup() {
        fakeIdentityV2Service.resetHandlers()
    }

    @Unroll
    def "When the tenant ID can be in multiple places in the request URI and it contains #uriSegment"() {
        given: "a configured Identity service"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = UUID.randomUUID().toString()
        }

        when: "a request is sent to repose with URI containing $uriSegment"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/$uriSegment/${fakeIdentityV2Service.client_tenantid}",
            method: 'GET',
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "everything with a valid Tenant ID that was extracted is sent to the origin service, otherwise it isn't"
        mc.receivedResponse.code as Integer == SC_OK

        mc.handlings.size() == 1
        mc.handlings[0].request.headers.findAll("x-tenant-id").get(0).contains(fakeIdentityV2Service.client_tenantid)

        where:
        uriSegment << ["serversOne", "serversTwo", "serversToo"]
    }

    def "When the tenant ID can be in multiple places in the request URI and contains serversBad"() {
        given: "a configured Identity service"
        fakeIdentityV2Service.with {
            client_token = UUID.randomUUID().toString()
            client_tenantid = UUID.randomUUID().toString()
        }

        when: "a request is sent to repose with URI containing serversBad"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/serversBad/${fakeIdentityV2Service.client_tenantid}",
            method: 'GET',
            headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "everything with a valid Tenant ID that was extracted is sent to the origin service, otherwise it isn't"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED
        mc.handlings.size() == 0
    }
}
