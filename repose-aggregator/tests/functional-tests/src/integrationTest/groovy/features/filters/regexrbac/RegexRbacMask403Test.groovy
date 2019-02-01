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
package features.filters.regexrbac

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES

@Category(Filters)
class RegexRbacMask403Test extends ReposeValveTest {

    static String GET = "GET"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/mask403", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "a request without a role necessary to access a resource should be rejected with a 404"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/simple",
            method: GET,
            headers: [(ROLES): "forbidden-role"]
        )

        then: "Repose responds with a 404"
        messageChain.handlings.size() == 0
        messageChain.receivedResponse.code as Integer == SC_NOT_FOUND
    }
}
