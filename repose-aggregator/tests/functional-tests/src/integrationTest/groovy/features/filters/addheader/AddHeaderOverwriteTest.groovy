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
 * Created by jennyvo on 12/15/14.
 */
@Category(Filters)
class AddHeaderOverwriteTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader", params)
        repose.configurationProvider.applyConfigs("features/filters/addheader/overwritetrue", params)
        repose.start()
    }

    def "When using add-header filter the expect header(s) in config is added to request/response"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1", "repose-test": "no-overwrite", "overwrite-test": "will-be-overwrite"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers])
        def reposehandling = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        reposehandling.request.headers.contains("x-rax-user")
        reposehandling.request.headers.getFirstValue("x-rax-user") == "test-user"
        reposehandling.request.headers.contains("x-rax-groups")
        reposehandling.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        reposehandling.request.headers.contains("repose-test")
        reposehandling.request.headers.findAll("repose-test").contains("no-overwrite")
        reposehandling.request.headers.findAll("repose-test").contains("this-is-a-test;q=0.5")
        reposehandling.request.headers.contains("overwrite-test")
        reposehandling.request.headers.getFirstValue("overwrite-test") == "this-is-overwrite-value;q=0.5"
        !reposehandling.request.headers.findAll("overwrite-test").contains("will-be-overwrite")
        mc.receivedResponse.headers.contains("response-header")
        mc.receivedResponse.headers.getFirstValue("response-header") == "foooo;q=0.9"
    }

    def "Add-header filter test with overwrite and quality"() {
        given:
        def Map headers = ["x-rax-user": "test-user", "x-rax-groups": "reposegroup1", "overwrite-quality-test": "not-overwrite;q=0.5"]

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers])
        def reposehandling = ((MessageChain) mc).getHandlings()[0]

        then: "The request/response should contain additional header from add-header config"
        reposehandling.request.headers.contains("x-rax-user")
        reposehandling.request.headers.getFirstValue("x-rax-user") == "test-user"
        reposehandling.request.headers.contains("x-rax-groups")
        reposehandling.request.headers.getFirstValue("x-rax-groups") == "reposegroup1"
        reposehandling.request.headers.contains("repose-test")
        reposehandling.request.headers.getFirstValue("repose-test") == "this-is-a-test;q=0.5"
        reposehandling.request.headers.contains("overwrite-test")
        reposehandling.request.headers.getFirstValue("overwrite-test") == "this-is-overwrite-value;q=0.5"
        reposehandling.request.headers.contains("overwrite-quality-test")
        reposehandling.request.headers.getFirstValue("overwrite-quality-test") == "this-is-overwrite-value;q=0.2"
    }

    def "When add response header with overwrite true"() {
        given:
        def Map headers = ["response-header": "will-be-overwrite;q=0.5"]
        def customeHandler = { return new Response(200, "OK", headers, "this is add header test") }

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint, headers: headers, defaultHandler: customeHandler])

        then:
        mc.receivedResponse.headers.contains("response-header")
        mc.receivedResponse.headers.getFirstValue("response-header") == "foooo;q=0.9"

    }

}
