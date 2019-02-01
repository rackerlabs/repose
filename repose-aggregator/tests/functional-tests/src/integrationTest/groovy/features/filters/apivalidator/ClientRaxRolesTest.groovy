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
 * A test to verify that a user can validate roles via api-checker
 * by setting the enable-rax-roles attribute on a validator.
 */
@Category(XmlParsing)
class ClientRaxRolesTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/clientraxroles/client_a", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("client_a:method=#method,headers=#headers,request=#request expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl method level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + request, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | request                  | headers                                    | responseCode
        "GET"    | "/v1"                    | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1"                    | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1"                    | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1"                    | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1"                    | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1"                    | ["x-roles": "random_observer"]             | "403"
        "GET"    | "/v1"                    | ["x-roles": "client_a:observer"]           | "403"
        "GET"    | "/v1"                    | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1"                    | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1"                    | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1"                    | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1"                    | ["x-roles": "client_a:admin"]              | "405"
        "PUT"    | "/v1"                    | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1"                    | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1"                    | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/application.wadl"   | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "random_observer"]             | "403"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "client_a:observer"]           | "403"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/application.wadl"   | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/application.wadl"   | ["x-roles": "client_a:admin"]              | "405"
        "PUT"    | "/v1/application.wadl"   | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/application.wadl"   | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/application.wadl"   | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/methods"            | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/methods"            | ["x-roles": "random_observer"]             | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "client_a:observer"]           | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/methods"            | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/methods"            | ["x-roles": "client_a:admin"]              | "200"
        "POST"   | "/v1/methods"            | ["x-roles": "random_observer"]             | "403"
        "POST"   | "/v1/methods"            | ["x-roles": "client_a:observer"]           | "403"
        "PUT"    | "/v1/methods"            | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/methods"            | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/methods"            | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/methods/test"       | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/methods/test"       | ["x-roles": "random_observer"]             | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "client_a:observer"]           | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/methods/test"       | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/methods/test"       | ["x-roles": "client_a:admin"]              | "405"
        "PUT"    | "/v1/methods/test"       | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/methods/test"       | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/methods/test"       | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/methods/default"    | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/methods/default"    | ["x-roles": "random_observer"]             | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "client_a:observer"]           | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/methods/default"    | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/methods/default"    | ["x-roles": "client_a:admin"]              | "405"
        "POST"   | "/v1/methods/default"    | ["x-roles": "random_observer"]             | "405"
        "POST"   | "/v1/methods/default"    | ["x-roles": "client_a:observer"]           | "405"
        "PUT"    | "/v1/methods/default"    | ["x-roles": "client_a:admin"]              | "200"
        "PUT"    | "/v1/methods/default"    | ["x-roles": "random_observer"]             | "403"
        "PUT"    | "/v1/methods/default"    | ["x-roles": "client_a:observer"]           | "403"
        "DELETE" | "/v1/methods/default"    | ["x-roles": "client_a:admin"]              | "200"
        "DELETE" | "/v1/methods/default"    | ["x-roles": "random_observer"]             | "403"
        "DELETE" | "/v1/methods/default"    | ["x-roles": "client_a:observer"]           | "403"
        "PATCH"  | "/v1/methods/default"    | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/methods/default"    | ["x-roles": "random_observer"]             | "405"
        "PATCH"  | "/v1/methods/default"    | ["x-roles": "client_a:observer"]           | "405"

        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "random_observer"]             | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "client_a:observer"]           | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/methods?limit=101"  | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/methods?limit=101"  | ["x-roles": "client_a:admin"]              | "200"
        "POST"   | "/v1/methods?limit=101"  | ["x-roles": "random_observer"]             | "403"
        "POST"   | "/v1/methods?limit=101"  | ["x-roles": "client_a:observer"]           | "403"
        "PUT"    | "/v1/methods?limit=101"  | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/methods?limit=101"  | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/methods?limit=101"  | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/methods?marker=101" | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "random_observer"]             | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "client_a:observer"]           | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/methods?marker=101" | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/methods?marker=101" | ["x-roles": "client_a:admin"]              | "200"
        "POST"   | "/v1/methods?marker=101" | ["x-roles": "random_observer"]             | "403"
        "POST"   | "/v1/methods?marker=101" | ["x-roles": "client_a:observer"]           | "403"
        "PUT"    | "/v1/methods?marker=101" | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/methods?marker=101" | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/methods?marker=101" | ["x-roles": "client_a:admin"]              | "405"

        "GET"    | "/v1/supportedMethods"   | ["x-roles": "default,client_a:admin"]      | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "default,admin"]               | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "default,identity:user-admin"] | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "default,identity:admin"]      | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "default"]                     | "403"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "random_observer"]             | "403"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "client_a:observer"]           | "403"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "client_a:admin"]              | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "admin"]                       | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "identity:user-admin"]         | "200"
        "GET"    | "/v1/supportedMethods"   | ["x-roles": "identity:admin"]              | "200"
        "POST"   | "/v1/supportedMethods"   | ["x-roles": "client_a:admin"]              | "405"
        "POST"   | "/v1/supportedMethods"   | ["x-roles": "random_observer"]             | "405"
        "POST"   | "/v1/supportedMethods"   | ["x-roles": "client_a:observer"]           | "405"
        "PUT"    | "/v1/supportedMethods"   | ["x-roles": "client_a:admin"]              | "405"
        "DELETE" | "/v1/supportedMethods"   | ["x-roles": "client_a:admin"]              | "405"
        "PATCH"  | "/v1/supportedMethods"   | ["x-roles": "client_a:admin"]              | "405"

    }

}
