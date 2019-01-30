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
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES

@Category(Filters)
class RegexRbacExternalConfigurationTest extends ReposeValveTest {

    static String GET = "GET"
    static String POST = "POST"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/external", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "a #method request for #resource with roles #roles is handled when resources are defined externally to the configuration file"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: method,
            headers: [(ROLES): roles]
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == expectedHandlings
        messageChain.receivedResponse.code as Integer == expectedStatusCode

        where:
        resource     | method | roles   || expectedHandlings | expectedStatusCode
        "/simple"    | GET    | "role1" || 1                 | SC_OK
        "/simple"    | POST   | "role1" || 0                 | SC_METHOD_NOT_ALLOWED
        "/simple"    | GET    | "role3" || 0                 | SC_FORBIDDEN
        "/not/found" | GET    | "role1" || 0                 | SC_NOT_FOUND
    }
}
