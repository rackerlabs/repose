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
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class URIEncodingWithApiValidatorTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/URIEncode/withAPIValidator", params)
        repose.start(killOthersBeforeStarting: false, waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint, 120)
    }

    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "API Validator filter handles '+' in URI path"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent, method: "GET", headers: ["X-Roles": "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
        messageChain.handlings.get(0).request.path == URItoriginService

        where:
        URISent                    | URItoriginService
        "/+messages?ids=locations" | "/+messages?ids=locations"
        "/messages/+add-nodes"     | "/messages/+add-nodes"
    }
}
