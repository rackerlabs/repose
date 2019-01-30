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

package features.filters.addheader

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

@Category(Filters)
class AddHeaderRequestOnlyTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader/requestonly", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "When using add-header filter the expected header in config is added to request"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers])
        def sentRequest = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        sentRequest.request.headers.contains("x-rax-user")
        sentRequest.request.headers.getFirstValue("x-rax-user") == "test-user"
        sentRequest.request.headers.contains("x-rax-groups")
        sentRequest.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        sentRequest.request.headers.contains("repose-test")
        sentRequest.request.headers.getFirstValue("repose-test") == "this-is-a-test"
        !mc.getReceivedResponse().headers.contains("response-header")
    }

    def "When using add-header filter the expected origin service response code should not change"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers,
                                      defaultHandler: { return new Response(302, "Redirect") }])
        def sentRequest = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        sentRequest.request.headers.contains("x-rax-user")
        sentRequest.request.headers.getFirstValue("x-rax-user") == "test-user"
        sentRequest.request.headers.contains("x-rax-groups")
        sentRequest.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        sentRequest.request.headers.contains("repose-test")
        sentRequest.request.headers.getFirstValue("repose-test") == "this-is-a-test"
        !mc.getReceivedResponse().headers.contains("response-header")
        mc.receivedResponse.code == "302"
    }
}
