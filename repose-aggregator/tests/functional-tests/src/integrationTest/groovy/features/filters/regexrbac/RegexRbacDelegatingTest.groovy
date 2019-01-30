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
class RegexRbacDelegatingTest extends ReposeValveTest {

    static String GET = "GET"
    static String POST = "POST"

    static String DELEGATED_HEADER = "X-Delegated"
    static String CONFIGURED_DELEGATING_QUALITY = "0.75"
    static String CONFIGURED_DELEGATING_COMPONENT_NAME = "rbac-delegator"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        params += [
            delegationQuality      : CONFIGURED_DELEGATING_QUALITY,
            delegatingComponentName: CONFIGURED_DELEGATING_COMPONENT_NAME
        ]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/common", params)
        repose.configurationProvider.applyConfigs("features/filters/regexrbac/delegating", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "a delegation header should be added for #expectedReason"() {
        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + resource,
            method: method,
            headers: [(ROLES): roles]
        )

        then: "the origin service responds with a 200"
        messageChain.handlings.size() == 1
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the delegation header with default values"
        messageChain.handlings[0].request.headers.contains(DELEGATED_HEADER)
        messageChain.handlings[0].request.headers.findAll(DELEGATED_HEADER) =~ delegationMsgPattern(expectedStatusCode)

        where:
        resource          | method | roles            | expectedStatusCode    | expectedReason
        "/not/a/resource" | GET    | "role1, role2"   | SC_NOT_FOUND          | "No Matching Paths"
        "/simple"         | POST   | "role1, role2"   | SC_METHOD_NOT_ALLOWED | "No Matching Methods"
        "/simple"         | GET    | "forbidden-role" | SC_FORBIDDEN          | "Non-Matching Roles"
    }

    String delegationMsgPattern(int code) {
        return "status_code=${code}.component=${CONFIGURED_DELEGATING_COMPONENT_NAME}.message=[^;]+;q=${CONFIGURED_DELEGATING_QUALITY}"
    }
}
