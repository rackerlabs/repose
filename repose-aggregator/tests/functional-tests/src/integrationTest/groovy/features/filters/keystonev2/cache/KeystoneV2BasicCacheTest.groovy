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
package features.filters.keystonev2.cache

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Identity

@Category(Identity)
class KeystoneV2BasicCacheTest extends ReposeValveTest {

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)

        deproxy.addEndpoint(params.targetPort, 'origin service')
        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(port: params.identityPort, name: 'identity service', defaultHandler: fakeIdentityV2Service.handler)

        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        fakeIdentityV2Service.resetDefaultParameters()
    }

    def "The X-Auth-Token-Key header is sent whether or not the token is cached"() {
        when: "the token is retrieved from Keystone v2"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "the X-Auth-Token-Key request header is sent"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token-Key") == "IDENTITY:V2:TOKEN:${fakeIdentityV2Service.client_token}"

        when: "the token is retrieved from the cache"
        mc = deproxy.makeRequest(
            url: reposeEndpoint + "/servers/test",
            headers: ['X-Auth-Token': fakeIdentityV2Service.client_token])

        then: "the X-Auth-Token-Key request header is sent"
        fakeIdentityV2Service.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token-Key") == "IDENTITY:V2:TOKEN:${fakeIdentityV2Service.client_token}"
    }
}
