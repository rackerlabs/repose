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
package features.core.powerfilter

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

/**
 * Created by adrian on 8/19/16.
 */
@Category(Core)
class FilterChainBypassTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/bypass", params)
        repose.start()
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    def "when you go through the bypass url the filter chain is skipped"() {
        when: "a request is made to the bypass"
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint + "/healthcheck/banana")

        then: "the header from the filter chain shouldn't have been added"
        mc1.handlings[0].request.headers.findAll("repose-test").isEmpty()

        when: "A request is made to a different resource"
        MessageChain mc2 = deproxy.makeRequest(url: reposeEndpoint + "/banana/phone")

        then: "the header from the filter chain should be present"
        mc2.handlings[0].request.headers.getFirstValue("repose-test") == "this-is-a-test"
    }
}
