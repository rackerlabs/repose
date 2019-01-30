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

/**
 * Created by jennyvo on 12/11/14.
 */
@Category(Filters)
class AddHeaderTest extends ReposeValveTest {

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "When using add-header filter the expect header(s) in config is added to request/response"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)
        def sentRequest = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        sentRequest.request.headers.contains("x-rax-user")
        sentRequest.request.headers.getFirstValue("x-rax-user") == "test-user"
        sentRequest.request.headers.contains("x-rax-groups")
        sentRequest.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        sentRequest.request.headers.contains("repose-test")
        sentRequest.request.headers.getFirstValue("repose-test") == "this-is-a-test"
        mc.getReceivedResponse().headers.contains("response-header")
        mc.getReceivedResponse().headers.getFirstValue("response-header") == "foooo;q=0.9"
    }

    def "When using add-header filter the expected origin service response code should not change"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers,
                defaultHandler: { new Response(302, "Redirect") })
        def sentRequest = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        sentRequest.request.headers.contains("x-rax-user")
        sentRequest.request.headers.getFirstValue("x-rax-user") == "test-user"
        sentRequest.request.headers.contains("x-rax-groups")
        sentRequest.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        sentRequest.request.headers.contains("repose-test")
        sentRequest.request.headers.getFirstValue("repose-test") == "this-is-a-test"
        mc.getReceivedResponse().headers.contains("response-header")
        mc.getReceivedResponse().headers.getFirstValue("response-header") == "foooo;q=0.9"
        mc.receivedResponse.code == "302"
    }

    def "Add header filter will add headers to request/response even if headers exist"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1", "repose-test": "test1"]
        def Map respheader = ["test": "test", "response-header": "original"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers,
                defaultHandler: { new Response(200, "OK", respheader) })
        def sentRequest = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        sentRequest.request.headers.contains("x-rax-user")
        sentRequest.request.headers.getFirstValue("x-rax-user") == "test-user"
        sentRequest.request.headers.contains("x-rax-groups")
        sentRequest.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        sentRequest.request.headers.contains("repose-test")
        sentRequest.request.headers.findAll("repose-test").contains("this-is-a-test")
        sentRequest.request.headers.findAll("repose-test").contains("test1")
        mc.getReceivedResponse().headers.contains("response-header")
        mc.getReceivedResponse().headers.findAll("test").contains("test")
        mc.getReceivedResponse().headers.findAll("response-header").contains("original")
        mc.getReceivedResponse().headers.findAll("response-header").contains("foooo;q=0.9")
        mc.receivedResponse.code == "200"
    }

    def "Verify miss config add header - no header"() {
        given:
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader/noheader", params)
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1", "repose-test": "test1"]

        when:
        repose.start()
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers,
                defaultHandler: { new Response(200, "OK") })

        then:
        reposeLogSearch.searchByString("At least one header must be defined.")
        mc.receivedResponse.code == "503"
    }
}
