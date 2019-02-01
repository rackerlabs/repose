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
class InvalidURITest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy/invaliduri", params)
        repose.start()
        waitUntilReadyToServiceRequests()

    }

    @Unroll("when given a uri with invalid characters, Repose should return a 400: #uriSuffixGiven with #method")
    def "when given a uri with a invalid characters, Repose should return a 400"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: "/path/" + uriSuffixGiven, method: method)

        then:
        messageChain.receivedResponse.code == "400"
        messageChain.receivedResponse.message == 'Error processing request'

        where:
        // Deproxy currently does not support non-UTF-8 characters, so only invalid UTF-8 characters are tested
        [uriSuffixGiven, method] <<
                [['[', ']', '{', '}', '`', '^', '|', '\\', '<', '>'],
                 ["POST", "GET", "PUT", "DELETE", "TRACE", "OPTIONS", "PATCH"]].combinations()
    }
}
