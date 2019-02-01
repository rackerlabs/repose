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

package features.core.proxy

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class InvalidRequestMethodTest extends ReposeValveTest {
    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getTargetPort())
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()
    }

    @Unroll
    def "Should return 405 when method name (#method) is invalid for request"() {
        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, method: method])

        then:
        mc.receivedResponse.code == "405"

        where:
        method << ["pull", "derp", "invalid"]
    }

    @Unroll
    def "Should return 200 when method name (#method) is valid for request"() {
        when:
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint, method: method])

        then:
        mc.receivedResponse.code == "200"

        where:
        method << ["get", "post", "put", "delete", "head", "options", "patch", "trace"]
    }
}
