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
package features.filters.compression

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

@Category(Filters)
class CompressionResponseCodeTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(PortFinder.instance.getNextOpenPort())

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/compression", params)
        repose.start()
    }

    def "when decompression fails with EOF Exception, return 400"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST",
                headers: ["Content-Encoding": "gzip"],
                requestBody: "")

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code == "400"
    }

    def "when decompression fails with EOF Exception and Content-Length is set to 0, return 400"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST",
                headers: ["Content-Encoding": "gzip", "Content-Length": "0"],
                requestBody: "")

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code == "400"
    }
}
