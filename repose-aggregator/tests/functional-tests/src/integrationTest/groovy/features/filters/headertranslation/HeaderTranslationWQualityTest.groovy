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
package features.filters.headertranslation

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 3/22/16.
 *  Header Translation With Quality
 */
class HeaderTranslationWQualityTest extends ReposeValveTest {
    def static Map params = [:]
    def static originEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params);
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/wquality", params);
        repose.start()
    }

    def "When translate header with quality"() {

        when: "Send request with headers to translate"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers:
                ["x-pp-user": "a", "x-tenant-name": "b", "x-roles": "c"])
        def reposehandling = mc.getHandlings()[0]

        then: "new header should include quality"
        reposehandling.request.headers.contains("x-pp-user")
        reposehandling.request.headers.contains("x-rax-username")
        !reposehandling.request.headers.contains("x-tenant-name")
        reposehandling.request.headers.contains("x-roles")
        reposehandling.request.headers.contains("x-rax-roles")
        reposehandling.request.headers.getFirstValue("x-rax-username") == "a;q=0.5"
        reposehandling.request.headers.getFirstValue("x-rax-roles") == "c;q=0.2"
        reposehandling.request.headers.getFirstValue("x-roles") == "c"
        reposehandling.request.headers.getFirstValue("x-pp-user") == "a"
    }

    def "the original header is splittable but isn't configured to, all new translated header will be added with quality"() {
        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["x-pp-user"    : "test, repose",
                          "x-tenant-name": "b",
                          "x-roles"      : "test"]
        )
        def reposehandling = mc.getHandlings()[0]

        then:
        reposehandling.request.getHeaders().contains("x-rax-username")
        reposehandling.request.getHeaders().findAll("x-rax-username").contains("test, repose;q=0.5")
        reposehandling.request.getHeaders().contains("x-roles")
        reposehandling.request.getHeaders().getFirstValue("x-roles") == "test"
    }

    def "when translation header quality in config should override quality from header"() {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["x-pp-user"    : "a",
                          "x-tenant-name": "b",
                          "x-roles"      : "c;q=0.3"]
        )

        def reposehandling = mc.getHandlings()[0]

        then: "origin receives translated headers"
        reposehandling.request.getHeaders().contains("x-rax-username")
        reposehandling.request.getHeaders().contains("x-rax-tenants")
        reposehandling.request.getHeaders().contains("x-rax-roles")
        reposehandling.request.getHeaders().getFirstValue("x-rax-roles") == "c;q=0.2"
        reposehandling.request.getHeaders().contains("x-pp-user")
        !reposehandling.request.getHeaders().contains("x-tenant-name")
        reposehandling.request.getHeaders().contains("x-roles")
        reposehandling.request.getHeaders().getFirstValue("x-roles") == "c;q=0.3"
    }

    def "when translation header with different quality header"() {

        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["x-pp-user"    : "a",
                          "x-tenant-name": "b",
                          "x-roles"      : "test",
                          "x-rax-roles"  : "test;q=0.5"]
        )

        def reposehandling = mc.getHandlings()[0]

        then: "origin receives translated headers"
        reposehandling.request.getHeaders().contains("x-rax-username")
        reposehandling.request.getHeaders().contains("x-rax-tenants")
        reposehandling.request.getHeaders().contains("x-rax-roles")
        reposehandling.request.getHeaders().findAll("x-rax-roles").contains("test;q=0.2")
        reposehandling.request.getHeaders().findAll("x-rax-roles").contains("test;q=0.5")
        reposehandling.request.getHeaders().contains("x-pp-user")
        !reposehandling.request.getHeaders().contains("x-tenant-name")
        reposehandling.request.getHeaders().contains("x-roles")
        reposehandling.request.getHeaders().getFirstValue("x-roles") == "test"
    }

    def "Verify splittable option, all new translated headers will be added with quality"() {
        when: "client passes a request through repose with headers to be translated"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["test-headers"    : "test, repose",
                          "nonsplit-headers": "a, b, c",]
        )
        def reposehandling = mc.getHandlings()[0]

        then:
        reposehandling.request.getHeaders().contains("test-headers")
        reposehandling.request.getHeaders().contains("repose-headers")
        reposehandling.request.getHeaders().getFirstValue("test-headers") == ("test, repose")
        reposehandling.request.getHeaders().findAll("repose-headers").contains("test;q=0.3")
        reposehandling.request.getHeaders().findAll("repose-headers").contains("repose;q=0.3")
        reposehandling.request.getHeaders().contains("nonsplit-headers")
        reposehandling.request.getHeaders().contains("repose-nonsplit")
        reposehandling.request.getHeaders().getFirstValue("nonsplit-headers") == "a, b, c"
        reposehandling.request.getHeaders().getFirstValue("repose-nonsplit") == "a, b, c;q=0.2"
    }
}
