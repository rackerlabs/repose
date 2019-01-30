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
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/3/14.
 */
@Category(XmlParsing)
class ApiValidatorDelegatingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def static params

    /*
        When delegating is set to true, the invalid/fail request will be forwarded to
        - next filter (if exists) with failed message
        - to origin service with failed message and up to origin service handle
    */

    @Unroll("Delegating:headers=#headers, failed message=#delegateMsg")
    def "when delegating is true, Repose can delegate invalid request with failed reason to origin service handle"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        method   | path | headers                                  | responseCode | delegateMsg
        "GET"    | "/a" | ["x-roles": "raxrole-test1"]             | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{a};q=0.5"
        "PUT"    | "/a" | ["x-roles": "raxrole-test1, a:admin"]    | "200"        | "status_code=405`component=api-validator`message=Bad method: PUT. The Method does not match the pattern: 'DELETE|GET|POST';q=0.5"
        "POST"   | "/a" | ["x-roles": "raxrole-test1, a:observer"] | "200"        | "status_code=405`component=api-validator`message=Bad method: POST. The Method does not match the pattern: 'GET';q=0.5"
        "POST"   | "/a" | ["x-roles": "raxrole-test1, a:bar"]      | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{a};q=0.5"
        "GET"    | "/b" | ["x-roles": "raxrole-test2"]             | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{b};q=0.5"
        "PUT"    | "/b" | ["x-roles": "raxrole-test2"]             | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{b};q=0.5"
        "PUT"    | "/b" | ["x-roles": "raxrole-test2, b:observer"] | "200"        | "status_code=405`component=api-validator`message=Bad method: PUT. The Method does not match the pattern: 'GET';q=0.5"
        "DELETE" | "/b" | ["x-roles": "raxrole-test2, b:bar"]      | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{b};q=0.5"
        "POST"   | "/b" | ["x-roles": "raxrole-test2, b:admin"]    | "200"        | "status_code=405`component=api-validator`message=Bad method: POST. The Method does not match the pattern: 'DELETE|GET|PUT';q=0.5"
        "PUT"    | "/b" | ["x-roles": "raxrole-test2, a:admin"]    | "200"        | "status_code=404`component=api-validator`message=Resource not found: /{b};q=0.5"
    }
}
