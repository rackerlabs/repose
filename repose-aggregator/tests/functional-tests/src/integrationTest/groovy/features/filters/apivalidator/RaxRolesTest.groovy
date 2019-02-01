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
class RaxRolesTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/raxroles", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    /*
        when enable-rax-role is set to false, all user roles will allow to access all methods
        available in wadl
     */

    @Unroll("raxRolesDisabled:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-role is false, user authorized to access the entire wadl"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                                     | responseCode
        "GET"    | ["x-roles": "raxRolesDisabled, allroles"]   | "200"
        "GET"    | ["x-roles": "raxRolesDisabled, a:observer"] | "200"
        "GET"    | ["x-roles": "raxRolesDisabled"]             | "200"
        "PUT"    | ["x-roles": "raxRolesDisabled"]             | "200"
        "POST"   | ["x-roles": "raxRolesDisabled"]             | "200"
        "DELETE" | ["x-roles": "raxRolesDisabled"]             | "200"
        "PATCH"  | ["x-roles": "raxRolesDisabled"]             | "405"
    }
    /*
        When enable-rax-role is set to true, certain user roles will allow to access certain methods
        according to config in the wadl.
        i.e. 'GET' method only be available to access by a:observer and a:admin role
     */

    @Unroll("raxRolesEnabled:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl method level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                                              | responseCode
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer"]           | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]    | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]       | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]              | "200"
        "GET"    | ["x-roles": "raxRolesEnabled"]                       | "403"
        "GET"    | ["x-roles": "raxRolesEnabled, a:creator"]            | "403"
        "GET"    | ["x-roles": "role-with-dashes, This-Is-A-Role"]      | "200"
        "GET"    | ["x-roles": "role-with-dashes, This,Is,A,Role"]      | "403"
        "GET"    | ["x-roles": "role-with-spaces, a:admin wsp"]         | "200"
        "GET"    | ["x-roles": "role-with-spaces, This Is A Role"]      | "200"
        "GET"    | ["x-roles": "role-with-spaces, This,Is,A,Role"]      | "403"
        "GET"    | ["x-roles": "role-with-spaces, This Is A&#xA0;Role"] | "403"
        "GET"    | ["x-roles": "role-with-spaces, a:admin"]             | "403"
        "GET"    | null                                                 | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:admin"]              | "200"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]       | "200"
        "POST"   | ["x-roles": "raxRolesEnabled"]                       | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:observer"]           | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar"]                | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:observer"]    | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:creator"]            | "403"
        "POST"   | ["x-roles": "role-with-dashes, This-Is-A-Role"]      | "403"
        "POST"   | ["x-roles": "role-with-spaces, This Is A Role"]      | "403"
        "POST"   | ["x-roles": "role-with-spaces, a:admin wsp"]         | "200"
        "POST"   | ["x-roles": "role-with-spaces, a:admin, wsp"]        | "403"
        "POST"   | null                                                 | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin"]              | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin, a:bar"]       | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]       | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:admin"]  | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled"]                       | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar"]                | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]     | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, observer, creator"]    | "403"
        "DELETE" | ["x-roles": "role-with-dashes, This-Is-A-Role"]      | "403"
        "DELETE" | ["x-roles": "role-with-spaces, This Is A Role"]      | "403"
        "DELETE" | ["x-roles": "role-with-spaces, a:admin wsp"]         | "200"
        "DELETE" | null                                                 | "403"
        "PUT"    | ["x-roles": "raxRolesEnabled"]                       | "405"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:admin"]              | "405"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar"]                | "405"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]    | "405"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]     | "405"
        "PUT"    | ["x-roles": "role-with-dashes, This-Is-A-Role"]      | "405"
        "PUT"    | ["x-roles": "role-with-spaces, This Is A Role"]      | "405"
        "PUT"    | ["x-roles": "role-with-spaces, a:admin wsp"]         | "405"
    }

    /*
        When enable-rax-role is set to true, and roles set resource level will have access all methods
        and certain user roles set at method level will allow to access certain methods in the wadl.
        i.e. a:admin role in this setting will have access to all methods
     */

    @Unroll("User3:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl resource level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method,
                headers: headers)
        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                                        | responseCode
        "GET"    | ["x-roles": "test_user3, a:admin"]             | "200"
        "GET"    | ["x-roles": "test_user3, a:observer"]          | "200"
        "GET"    | ["x-roles": "test_user3"]                      | "403"
        "GET"    | ["x-roles": "test_user3, b:creator"]           | "403"
        "PUT"    | ["x-roles": "test_user3, a:admin"]             | "200"
        "PUT"    | ["x-roles": "test_user3, a:creator"]           | "200"
        "PUT"    | ["x-roles": "test_user3, a:observer, a:admin"] | "200"
        "PUT"    | ["x-roles": "test_user3, a:bar"]               | "403"
        "PUT"    | ["x-roles": "test_user3"]                      | "403"
        "PUT"    | ["x-roles": "test_user3, a:observe"]           | "403"
        "POST"   | ["x-roles": "test_user3, a:admin"]             | "200"
        "POST"   | ["x-roles": "test_user3"]                      | "403"
        "POST"   | ["x-roles": "test_user3, a:observer"]          | "403"
        "DELETE" | ["x-roles": "test_user3, a:admin"]             | "200"
        "DELETE" | ["x-roles": "test_user3, a:creator"]           | "200"
        "DELETE" | ["x-roles": "test_user3, a:observer, a:admin"] | "200"
        "DELETE" | ["x-roles": "test_user3, a:creator"]           | "200"
        "DELETE" | ["x-roles": "test_user3, a:bar, a:creator"]    | "200"
        "DELETE" | ["x-roles": "test_user3"]                      | "403"
        "DELETE" | ["x-roles": "test_user3, a:bar"]               | "403"
        "DELETE" | ["x-roles": "test_user3, a:observe"]           | "403"
        "GET"    | null                                           | "403"
        "PUT"    | null                                           | "403"
        "POST"   | null                                           | "403"
        "DELETE" | null                                           | "403"
    }
    /*
        When enable-rax-role is set to true, and roles set resource level will have access all methods
        and certain user roles set at method level will allow to access certain methods in the wadl.
        i.e. a:admin role in this setting will have access to all methods
     */

    @Unroll("User4:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true and wadl set up role from multiple resource level"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                        | responseCode
        "GET"    | "/a"   | ["x-roles": "test_user4, a:admin"]             | "200"
        "GET"    | "/a"   | ["x-roles": "test_user4, a:observer"]          | "200"
        "GET"    | "/a"   | ["x-roles": "test_user4, a:observer, a:bar"]   | "200"
        "GET"    | "/a"   | ["x-roles": "test_user4, a:bar, a:admin"]      | "200"
        "GET"    | "/a"   | ["x-roles": "test_user4, a:bar"]               | "403"
        "GET"    | "/a"   | ["x-roles": "test_user4, a:creator"]           | "403"
        "GET"    | "/a"   | ["x-roles": "test_user4"]                      | "403"
        "GET"    | "/a/b" | ["x-roles": "test_user4, a:admin"]             | "200"
        "GET"    | "/a/b" | ["x-roles": "test_user4, b:observer"]          | "200"
        "GET"    | "/a/b" | ["x-roles": "test_user4, b:creator"]           | "403"
        "GET"    | "/b"   | ["x-roles": "test_user4, a:admin"]             | "404"
        "GET"    | "/a/b" | ["x-roles": "test_user4, a:observer"]          | "403"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:admin"]             | "200"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:creator"]           | "200"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:foo, a:creator"]    | "200"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:foo, a:admin"]      | "200"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:foo, b:creator"]    | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user4, a:admin"]             | "405"
        "POST"   | "/a/b" | ["x-roles": "test_user4, a:creator"]           | "405"
        "POST"   | "/a/c" | ["x-roles": "test_user4, a:creator"]           | "404"
        "POST"   | "/x"   | ["x-roles": "test_user4, a:admin"]             | "404"
        "POST"   | "/b"   | ["x-roles": "test_user4, a:creator"]           | "404"
        "POST"   | "/a"   | ["x-roles": "test_user4, b:creator"]           | "403"
        "POST"   | "/a"   | ["x-roles": "test_user4, a:observer"]          | "403"
        "POST"   | "/a"   | ["x-roles": "test_user4"]                      | "403"
        "POST"   | "/a"   | null                                           | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user4, a:admin"]             | "405"
        "PUT"    | "/a"   | ["x-roles": "test_user4"]                      | "405"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, a:admin, b:observer"] | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, a:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, b:observer, a:admin"] | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user4, b:observer"]          | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user4"]                      | "403"
        "PUT"    | "/a/c" | ["x-roles": "test_user4, b:creator"]           | "404"
        "PUT"    | "/b"   | ["x-roles": "test_user4, b:creator"]           | "404"
        "DELETE" | "/a"   | ["x-roles": "test_user4, a:admin"]             | "405"
        "DELETE" | "/b"   | ["x-roles": "test_user4, a:admin"]             | "404"
        "DELETE" | "/a/b" | ["x-roles": "test_user4, a:admin"]             | "200"
        "DELETE" | "/a/b" | ["x-roles": "test_user4, b:creator"]           | "200"
        "DELETE" | "/a/b" | ["x-roles": "test_user4, b:admin"]             | "200"
        "DELETE" | "/a/b" | ["x-roles": "test_user4, b:observer"]          | "403"
        "DELETE" | "/a/b" | ["x-roles": "test_user4, a:creator"]           | "200"
        "DELETE" | "/a/b" | null                                           | "403"
        "DELETE" | "/a/c" | ["x-roles": "test_user4, b:creator"]           | "404"
    }
    /*
        When enable-rax-role is set to true, and wadl has roles with #all will open to access by
        all roles.
        i.e. GET method /a will be no restriction and GET /b will be no restriction
     */

    @Unroll("User5:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true and wadl has roles with #all"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path    | headers                                      | responseCode
        "GET"    | "/a"    | ["x-roles": "test_user5, a:observer"]        | "200"
        "GET"    | "/a"    | ["x-roles": "test_user5, a:observer, a:bar"] | "200"
        "GET"    | "/a"    | ["x-roles": "test_user5, a:bar"]             | "200"
        "GET"    | "/a"    | ["x-roles": "test_user5, a:abar, a:admin"]   | "200"
        "GET"    | "/a"    | ["x-roles": "test_user5, a:admin"]           | "200"
        "GET"    | "/a"    | ["x-roles": "test_user5"]                    | "200"
        "GET"    | "/a/aa" | ["x-roles": "test_user5"]                    | "200"
        "GET"    | "/a/aa" | ["x-roles": "test_user5, a:foo"]             | "200"
        "POST"   | "/a"    | ["x-roles": "test_user5, a:admin"]           | "200"
        "POST"   | "/a"    | ["x-roles": "test_user5, a:creator"]         | "200"
        "POST"   | "/a"    | ["x-roles": "test_user5, a:observer"]        | "403"
        "PUT"    | "/a"    | ["x-roles": "test_user5, a:admin"]           | "405"
        "PUT"    | "/a/aa" | ["x-roles": "test_user5, a:bar, a:admin"]    | "200"
        "PUT"    | "/a/aa" | ["x-roles": "test_user5, a:bar"]             | "403"
        "PUT"    | "/a/aa" | ["x-roles": "test_user5"]                    | "403"
        "DELETE" | "/a"    | ["x-roles": "test_user5, a:admin"]           | "405"
        "DELETE" | "/a/aa" | ["x-roles": "test_user5, a:admin"]           | "200"
        "DELETE" | "/a/aa" | ["x-roles": "test_user5, a:foo"]             | "403"
        "DELETE" | "/a/aa" | ["x-roles": "test_user5, a:creator"]         | "403"
        "DELETE" | "/a/aa" | ["x-roles": "test_user5"]                    | "403"
        "GET"    | "/b"    | ["x-roles": "test_user5, a:admin"]           | "200"
        "GET"    | "/b"    | ["x-roles": "test_user5"]                    | "200"
        "GET"    | "/b"    | ["x-roles": "test_user5, bar"]               | "200"
        "GET"    | "/b/c"  | ["x-roles": "test_user5, c:admin"]           | "200"
        "GET"    | "/b/c"  | ["x-roles": "test_user5, c:observer"]        | "200"
        "GET"    | "/b/c"  | ["x-roles": "test_user5, bar"]               | "200"
        "GET"    | "/b/c"  | ["x-roles": "test_user5"]                    | "200"
        "POST"   | "/b"    | ["x-roles": "test_user5, b:creator"]         | "200"
        "POST"   | "/b"    | ["x-roles": "test_user5, a:admin"]           | "200"
        "POST"   | "/b"    | ["x-roles": "test_user5"]                    | "200"
        "POST"   | "/b/c"  | ["x-roles": "test_user5, c:admin"]           | "405"
        "POST"   | "/b"    | ["x-roles": "test_user5, a:admin"]           | "200"
        "POST"   | "/b/c"  | ["x-roles": "test_user5"]                    | "405"
        "DELETE" | "/b"    | ["x-roles": "test_user5"]                    | "405"
        "DELETE" | "/b"    | ["x-roles": "test_user5, b:admin"]           | "405"
        "DELETE" | "/b/c"  | ["x-roles": "test_user5"]                    | "200"
        "DELETE" | "/b/c"  | ["x-roles": "test_user5, c:admin"]           | "200"
        "DELETE" | "/b/c"  | ["x-roles": "test_user5, c:creator"]         | "200"
        "DELETE" | "/a/c"  | ["x-roles": "test_user5, c:creator"]         | "404"

    }
    /*
        When enable-rax-role is set to false, and check-headers doesn't affect
        all method available in wadl will be accessible by
        all roles.
     */

    @Unroll("User7:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is false and check-headers does not affect it"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                | responseCode
        "POST"   | "/a"   | ["x-roles": "test_user7, a:noone"]     | "200"
        "POST"   | "/a"   | ["x-roles": "test_user7, a:creator"]   | "200"
        "POST"   | "/a"   | ["x-roles": "test_user7"]              | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user7"]              | "405"
        "PUT"    | "/a"   | ["x-roles": "test_user7"]              | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user7"]              | "405"
        "GET"    | "/a"   | ["x-roles": "test_user7"]              | "405"
        "GET"    | "/a/b" | ["x-roles": "test_user7, a:noone"]     | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user7, a:noone"]     | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user7"]              | "200"
        "DELETE" | "/a/b" | ["x-roles": "test_user7"]              | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user7.1, a:creator"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user7.1"]            | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user7.1"]            | "200"
        "DELETE" | "/a"   | ["x-roles": "test_user7.1"]            | "405"
        "DELETE" | "/a/b" | ["x-roles": "test_user7.1"]            | "200"
        "GET"    | "/b"   | ["x-roles": "test_user7.1, a:noone"]   | "404"

    }
    /*
        When enable-rax-role is set to true, rax roles will not inherit from siblings
        if the path, method are not set to require specific roles then will be accessible
        to all roles.
     */

    @Unroll("User8:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Rax Roles will not inherit from siblings"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method | path   | headers                                      | responseCode
        "PUT"  | "/a"   | ["x-roles": "test_user8, a:observer, a:bar"] | "403"
        "PUT"  | "/a"   | ["x-roles": "test_user8"]                    | "403"
        "GET"  | "/a"   | ["x-roles": "test_user8, a:admin"]           | "200"
        "GET"  | "/b"   | ["x-roles": "test_user8, a:noone"]           | "200"
        "GET"  | "/b"   | ["x-roles": "test_user8, a:creator"]         | "200"
        "GET"  | "/b/c" | ["x-roles": "test_user8"]                    | "200"
        "POST" | "/b/c" | ["x-roles": "test_user8"]                    | "200"
    }
    /*
        When enable-rax-role is set to true, wadl has nested resources, even though
        nested resource doesn't set to require any specific role but it still inherit
        from 'parent' resource (path).
     */

    @Unroll("User9:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Wadl has nested resources"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                        | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user9"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user9, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user9"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user9, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user9, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user9, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user9"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user9, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user9"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, observer"]            | "403"
    }
    /*
        When enable-rax-role is set to true with remove-dups true, wadl has nested resources,
        even though nested resource doesn't set to require any specific role but it still inherit
        from 'parent' resource (path).
        ??? don't really under stand how remove-dups work in this case.
     */

    @Unroll("User10:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Remove Duplications is true"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                         | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user10"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user10, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user10"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user10, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user10, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user10, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user10"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user10, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user10"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, observer"]            | "403"
    }
    /*
        When enable-rax-role is set to true with check-headers true, wadl has nested resources,
        even though nested resource doesn't set to require any specific role but it still inherit
        from 'parent' resource (path).
        ??? check-headers doesn't really make any different in this case.
     */

    @Unroll("User11:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Check Headers is false"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                         | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user11"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user11, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user11"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user11, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user11, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user11, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user11"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user11, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user11"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, observer"]            | "403"
    }
    /*
        When enable-rax-role is set to true, hrefs to methods outside of the resource
        resource should be adhered to when appropriate.
     */

    @Unroll("User12:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, hrefs to methods outside of the resource should be adhered to when appropriate"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path | headers                                | responseCode
        "GET"    | "/a" | ["x-roles": "test_user12, a:admin"]    | "200"
        "GET"    | "/a" | ["x-roles": "test_user12, a:observer"] | "200"
        "GET"    | "/a" | ["x-roles": "test_user12, a:foo"]      | "403"
        "GET"    | "/b" | ["x-roles": "test_user12, a:admin"]    | "404"
        "POST"   | "/a" | ["x-roles": "test_user12, a:admin"]    | "200"
        "POST"   | "/a" | ["x-roles": "test_user12, a:creator"]  | "200"
        "POST"   | "/a" | ["x-roles": "test_user12"]             | "403"
        "POST"   | "/b" | ["x-roles": "test_user12, a:admin"]    | "404"
        "PUT"    | "/a" | ["x-roles": "test_user12, a:admin"]    | "200"
        "PUT"    | "/a" | ["x-roles": "test_user12, a:creator"]  | "200"
        "PUT"    | "/a" | ["x-roles": "test_user12, a:foo"]      | "403"
        "PUT"    | "/b" | ["x-roles": "test_user12, a:admin"]    | "404"
        "DELETE" | "/a" | ["x-roles": "test_user12, a:admin"]    | "200"
        "DELETE" | "/a" | ["x-roles": "test_user12, a:creator"]  | "403"
        "DELETE" | "/a" | ["x-roles": "test_user12"]             | "403"
        "DELETE" | "/b" | ["x-roles": "test_user12, a:admin"]    | "404"
        "PATCH"  | "/a" | ["x-roles": "test_user12, a:admin"]    | "200"
        "PATCH"  | "/a" | ["x-roles": "test_user12, a:creator"]  | "403"
        "PATCH"  | "/a" | ["x-roles": "test_user12"]             | "403"
        "PATCH"  | "/b" | ["x-roles": "test_user12, a:admin"]    | "404"
        "PATCH"  | "/a" | ["x-roles": "test_user, a:admin"]      | "403"
        "PUT"    | "/a" | ["x-roles": "test_user, a:admin"]      | "403"
        "POST"   | "/a" | ["x-roles": "test_user, a:admin"]      | "403"
        "GET"    | "/a" | ["x-roles": "test_user, a:admin"]      | "403"
        "GET"    | "/v" | ["x-roles": "test_user12"]             | "200"
        "GET"    | "/v" | ["x-roles": "test_user12, a:admin"]    | "200"
        "GET"    | "/v" | ["x-roles": "test_user12, a:foo"]      | "200"
    }
}
