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

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 12/3/14.
 * Reason: API Checker - Repose is returning HTTP code 403 with response body of 404
 * This test check if issue fix
 */
class MaskRaxRolesFixTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/raxroles/maskraxrolesfix", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
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
        //def jsonbody = "{\"code\": 200, \"message\": \"not and error\"}"

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)
        if (responseCode != "200") {
            def body = messageChain.getReceivedResponse().body
            def jsonbody = new JsonSlurper().parseText(body)
            assert jsonbody.code.toString() == responseCode
        }

        where:
        method   | path       | headers                                          | responseCode
        "GET"    | "/a"       | ["x-roles": "test_user4, a:admin"]               | "200"
        "GET"    | "/a"       | ["x-roles": "test_user4, a:observer"]            | "200"
        "GET"    | "/a"       | ["x-roles": "test_user4, a:observer, a:bar"]     | "200"
        "GET"    | "/a"       | ["x-roles": "test_user4, a:bar, a:admin"]        | "200"
        "GET"    | "/a"       | ["x-roles": "test_user4, a:bar"]                 | "404"
        "GET"    | "/a"       | ["x-roles": "test_user4, a:creator"]             | "405"
        "GET"    | "/a"       | ["x-roles": "test_user4"]                        | "404"
        "GET"    | "/a/b"     | ["x-roles": "test_user4, a:admin"]               | "200"
        "GET"    | "/a/b"     | ["x-roles": "test_user4, b:observer"]            | "200"
        "GET"    | "/a/b"     | ["x-roles": "test_user4, b:creator"]             | "405"
        "GET"    | "/b"       | ["x-roles": "test_user4, a:admin"]               | "404"
        "GET"    | "/a/b"     | ["x-roles": "test_user4, a:observer"]            | "404"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:admin"]               | "200"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:creator"]             | "200"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:foo, a:creator"]      | "200"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:foo, a:admin"]        | "200"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:foo, b:creator"]      | "405"
        "POST"   | "/a/b"     | ["x-roles": "test_user4, a:admin"]               | "405"
        "POST"   | "/a/b"     | ["x-roles": "test_user4, a:creator"]             | "405"
        "POST"   | "/a/c"     | ["x-roles": "test_user4, a:creator"]             | "404"
        "POST"   | "/x"       | ["x-roles": "test_user4, a:admin"]               | "404"
        "POST"   | "/b"       | ["x-roles": "test_user4, a:creator"]             | "404"
        "POST"   | "/a"       | ["x-roles": "test_user4, b:creator"]             | "405"
        "POST"   | "/a"       | ["x-roles": "test_user4, a:observer"]            | "405"
        "POST"   | "/a"       | ["x-roles": "test_user4"]                        | "404"
        "POST"   | "/a"       | null                                             | "404"
        "PUT"    | "/a"       | ["x-roles": "test_user4, a:admin"]               | "405"   //PUT method is not available at this level
        "PUT"    | "/a"       | ["x-roles": "test_user4"]                        | "404"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, a:admin"]               | "200"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, a:admin, b:observer"]   | "200"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, a:creator"]             | "200"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, b:creator"]             | "200"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, b:observer, a:admin"]   | "200"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4, b:observer"]            | "405"
        "PUT"    | "/a/b"     | ["x-roles": "test_user4"]                        | "404"
        "PUT"    | "/a/c"     | ["x-roles": "test_user4, b:creator"]             | "404"
        "PUT"    | "/b"       | ["x-roles": "test_user4, b:creator"]             | "404"
        "DELETE" | "/a"       | ["x-roles": "test_user4, a:admin"]               | "405"
        "DELETE" | "/b"       | ["x-roles": "test_user4, a:admin"]               | "404"
        "DELETE" | "/a/b"     | ["x-roles": "test_user4, a:admin"]               | "200"
        "DELETE" | "/a/b"     | ["x-roles": "test_user4, b:creator"]             | "200"
        "DELETE" | "/a/b"     | ["x-roles": "test_user4, b:admin"]               | "200"
        "DELETE" | "/a/b"     | ["x-roles": "test_user4, b:observer"]            | "405"
        "DELETE" | "/a/b"     | ["x-roles": "test_user4, a:creator"]             | "200"
        "DELETE" | "/a/b"     | null                                             | "404"
        "DELETE" | "/a/c"     | ["x-roles": "test_user4, b:creator"]             | "404"
        "GET"    | "/header"  | ["x-roles": "test_user4", "X-Auth-Token": "foo"] | "404"
        "GET"    | "/header2" | ["x-roles": "test_user4", "X-Auth-Token": "foo"] | "405"
    }
}
