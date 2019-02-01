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
 * RBAC tests ported from python
 */
@Category(XmlParsing)
class RbacTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/rbac", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    @Unroll("#method #path with roles: #roles should return a 405 METHOD NOT ALLOWED response")
    def "A user with the right role to access a resource, attempts to use a method not allowed for their role"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code == "405"
        messageChain.handlings.size() == 0

        where:
        path                        | method   | roles
        "/widgets"                  | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets"                  | "PUT"    | "cwaas:observer,cwaas:creator"
        "/widgets"                  | "PUT"    | "cwaas:observer,cwaas:admin"
        "/widgets"                  | "PUT"    | "cwaas:observer"
        "/widgets"                  | "PUT"    | "cwaas:creator,cwaas:admin"
        "/widgets"                  | "PUT"    | "cwaas:creator"
        "/widgets"                  | "PUT"    | "cwaas:admin"
        "/widgets"                  | "PUT"    | ""
        "/widgets"                  | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets"                  | "DELETE" | "cwaas:observer,cwaas:creator"
        "/widgets"                  | "DELETE" | "cwaas:observer,cwaas:admin"
        "/widgets"                  | "DELETE" | "cwaas:observer"
        "/widgets"                  | "DELETE" | "cwaas:creator,cwaas:admin"
        "/widgets"                  | "DELETE" | "cwaas:creator"
        "/widgets"                  | "DELETE" | "cwaas:admin"
        "/widgets"                  | "DELETE" | ""
        "/widgets/1234"             | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "POST"   | "cwaas:observer,cwaas:creator"
        "/widgets/1234"             | "POST"   | "cwaas:observer,cwaas:admin"
        "/widgets/1234"             | "POST"   | "cwaas:observer"
        "/widgets/1234"             | "POST"   | "cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "POST"   | "cwaas:creator"
        "/widgets/1234"             | "POST"   | "cwaas:admin"
        "/widgets/1234"             | "POST"   | ""
        "/gizmos"                   | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos"                   | "PUT"    | "cwaas:observer,cwaas:creator"
        "/gizmos"                   | "PUT"    | "cwaas:observer,cwaas:admin"
        "/gizmos"                   | "PUT"    | "cwaas:observer"
        "/gizmos"                   | "PUT"    | "cwaas:creator,cwaas:admin"
        "/gizmos"                   | "PUT"    | "cwaas:creator"
        "/gizmos"                   | "PUT"    | "cwaas:admin"
        "/gizmos"                   | "PUT"    | ""
        "/gizmos"                   | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos"                   | "DELETE" | "cwaas:observer,cwaas:creator"
        "/gizmos"                   | "DELETE" | "cwaas:observer,cwaas:admin"
        "/gizmos"                   | "DELETE" | "cwaas:observer"
        "/gizmos"                   | "DELETE" | "cwaas:creator,cwaas:admin"
        "/gizmos"                   | "DELETE" | "cwaas:creator"
        "/gizmos"                   | "DELETE" | "cwaas:admin"
        "/gizmos"                   | "DELETE" | ""
        "/gizmos/5678"              | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "POST"   | "cwaas:observer,cwaas:creator"
        "/gizmos/5678"              | "POST"   | "cwaas:observer,cwaas:admin"
        "/gizmos/5678"              | "POST"   | "cwaas:observer"
        "/gizmos/5678"              | "POST"   | "cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "POST"   | "cwaas:creator"
        "/gizmos/5678"              | "POST"   | "cwaas:admin"
        "/gizmos/5678"              | "POST"   | ""
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:observer"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:creator"
        "/widgets/1234/gizmos"      | "PUT"    | "cwaas:admin"
        "/widgets/1234/gizmos"      | "PUT"    | ""
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:observer"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:creator"
        "/widgets/1234/gizmos"      | "DELETE" | "cwaas:admin"
        "/widgets/1234/gizmos"      | "DELETE" | ""
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:observer"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:creator"
        "/widgets/1234/gizmos/5678" | "POST"   | "cwaas:admin"
        "/widgets/1234/gizmos/5678" | "POST"   | ""
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:observer"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:creator"
        "/widgets/1234/gizmos/5678" | "PUT"    | "cwaas:admin"
        "/widgets/1234/gizmos/5678" | "PUT"    | ""
    }

    @Unroll("#method #path with roles: #roles expecting: #expectedResponseCode")
    def "Original tests that were validating one of 3 possible responses: 403, 404, 405"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code == "405"
        messageChain.handlings.size() == 0

        where:
        path                        | method   | roles
        "/widgets"                  | "POST"   | "cwaas:observer"
        "/widgets"                  | "POST"   | ""
        "/widgets/1234"             | "PUT"    | "cwaas:observer"
        "/widgets/1234"             | "PUT"    | ""
        "/widgets/1234"             | "DELETE" | "cwaas:observer,cwaas:creator"
        "/widgets/1234"             | "DELETE" | "cwaas:observer"
        "/widgets/1234"             | "DELETE" | "cwaas:creator"
        "/widgets/1234"             | "DELETE" | ""
        "/gizmos"                   | "POST"   | "cwaas:observer"
        "/gizmos"                   | "POST"   | ""
        "/gizmos/5678"              | "PUT"    | "cwaas:observer"
        "/gizmos/5678"              | "PUT"    | ""
        "/gizmos/5678"              | "DELETE" | "cwaas:observer,cwaas:creator"
        "/gizmos/5678"              | "DELETE" | "cwaas:observer"
        "/gizmos/5678"              | "DELETE" | "cwaas:creator"
        "/gizmos/5678"              | "DELETE" | ""
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:observer"
        "/widgets/1234/gizmos"      | "POST"   | ""
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:observer"
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:creator"
        "/widgets/1234/gizmos/5678" | "DELETE" | ""

    }


    @Unroll("#method #path with roles: #roles expecting: #expectedResponseCode")
    def "A user with the right role to access a resource, uses a method allowed for their role"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1

        where:
        path                        | method   | roles
        "/widgets"                  | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets"                  | "GET"    | "cwaas:observer,cwaas:creator"
        "/widgets"                  | "GET"    | "cwaas:observer,cwaas:admin"
        "/widgets"                  | "GET"    | "cwaas:observer"
        "/widgets"                  | "GET"    | "cwaas:creator,cwaas:admin"
        "/widgets"                  | "GET"    | "cwaas:creator"
        "/widgets"                  | "GET"    | "cwaas:admin"
        "/widgets"                  | "GET"    | ""
        "/widgets"                  | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets"                  | "POST"   | "cwaas:observer,cwaas:creator"
        "/widgets"                  | "POST"   | "cwaas:observer,cwaas:admin"
        "/widgets"                  | "POST"   | "cwaas:creator,cwaas:admin"
        "/widgets"                  | "POST"   | "cwaas:creator"
        "/widgets"                  | "POST"   | "cwaas:admin"
        "/widgets/1234"             | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "GET"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234"             | "GET"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234"             | "GET"    | "cwaas:observer"
        "/widgets/1234"             | "GET"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "GET"    | "cwaas:creator"
        "/widgets/1234"             | "GET"    | "cwaas:admin"
        "/widgets/1234"             | "GET"    | ""
        "/widgets/1234"             | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "PUT"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234"             | "PUT"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234"             | "PUT"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "PUT"    | "cwaas:creator"
        "/widgets/1234"             | "PUT"    | "cwaas:admin"
        "/widgets/1234"             | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "DELETE" | "cwaas:observer,cwaas:admin"
        "/widgets/1234"             | "DELETE" | "cwaas:creator,cwaas:admin"
        "/widgets/1234"             | "DELETE" | "cwaas:admin"
        "/gizmos"                   | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos"                   | "GET"    | "cwaas:observer,cwaas:creator"
        "/gizmos"                   | "GET"    | "cwaas:observer,cwaas:admin"
        "/gizmos"                   | "GET"    | "cwaas:observer"
        "/gizmos"                   | "GET"    | "cwaas:creator,cwaas:admin"
        "/gizmos"                   | "GET"    | "cwaas:creator"
        "/gizmos"                   | "GET"    | "cwaas:admin"
        "/gizmos"                   | "GET"    | ""
        "/gizmos"                   | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos"                   | "POST"   | "cwaas:observer,cwaas:creator"
        "/gizmos"                   | "POST"   | "cwaas:observer,cwaas:admin"
        "/gizmos"                   | "POST"   | "cwaas:creator,cwaas:admin"
        "/gizmos"                   | "POST"   | "cwaas:creator"
        "/gizmos"                   | "POST"   | "cwaas:admin"
        "/gizmos/5678"              | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "GET"    | "cwaas:observer,cwaas:creator"
        "/gizmos/5678"              | "GET"    | "cwaas:observer,cwaas:admin"
        "/gizmos/5678"              | "GET"    | "cwaas:observer"
        "/gizmos/5678"              | "GET"    | "cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "GET"    | "cwaas:creator"
        "/gizmos/5678"              | "GET"    | "cwaas:admin"
        "/gizmos/5678"              | "GET"    | ""
        "/gizmos/5678"              | "PUT"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "PUT"    | "cwaas:observer,cwaas:creator"
        "/gizmos/5678"              | "PUT"    | "cwaas:observer,cwaas:admin"
        "/gizmos/5678"              | "PUT"    | "cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "PUT"    | "cwaas:creator"
        "/gizmos/5678"              | "PUT"    | "cwaas:admin"
        "/gizmos/5678"              | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "DELETE" | "cwaas:observer,cwaas:admin"
        "/gizmos/5678"              | "DELETE" | "cwaas:creator,cwaas:admin"
        "/gizmos/5678"              | "DELETE" | "cwaas:admin"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:observer"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:creator"
        "/widgets/1234/gizmos"      | "GET"    | "cwaas:admin"
        "/widgets/1234/gizmos"      | "GET"    | ""
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:creator"
        "/widgets/1234/gizmos"      | "POST"   | "cwaas:admin"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:observer,cwaas:creator"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:observer"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:creator"
        "/widgets/1234/gizmos/5678" | "GET"    | "cwaas:admin"
        "/widgets/1234/gizmos/5678" | "GET"    | ""
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:observer,cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:observer,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:creator,cwaas:admin"
        "/widgets/1234/gizmos/5678" | "DELETE" | "cwaas:admin"
    }
}
