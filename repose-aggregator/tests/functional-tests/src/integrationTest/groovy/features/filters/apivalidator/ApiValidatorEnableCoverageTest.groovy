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
 * Created by jennyvo on 6/11/14.
 *  This test verify api stage machine coverage
 */

@Category(XmlParsing)
class ApiValidatorEnableCoverageTest extends ReposeValveTest {
    String intrumentedHandler = 'com.rackspace.com.papi.components.checker:type=handler*,*'
    def static s0_count = 0
    def S0 = 0
    def SA = 0
    def S0_a_admin = 0

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/statemachine", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/statemachine/common", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def static params

    def setup() {
        reposeLogSearch.cleanLog()
    }

    /*
        When enable-api-coverage is set to true, enable-rax-role is set to true,
        certain user roles will allow to access certain methods according to config in the wadl.
        i.e. 'GET' method only be available to access by a:observer and a:admin role
        Also with enable-api-coverage set to true there should be paths logged to the api-coverage-logger.
    */
    @Unroll("enableapicoverage:headers=#headers,expected S0_a_admin:#S0_a_admin_count, SA:#SA_count")
    def "when enable-api-coverage is true, validate count at state level"() {
        given:
        File outputfile = new File("output.dot")
        if (outputfile.exists())
            outputfile.delete()
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)
        if (headers != null)
            s0_count = s0_count + 1

        def getBeanObj = repose.jmx.getMBeanNames(intrumentedHandler)
        getBeanObj.each {
            def name = it.getKeyProperty('name')
            if (name == "S0") {
                S0 = repose.jmx.getMBeanAttribute(it.toString(), "Count")
            } else if (name == "SA") {
                SA = repose.jmx.getMBeanAttribute(it.toString(), "Count")
            } else if (name == "S0_a_admin") {
                S0_a_admin = repose.jmx.getMBeanAttribute(it.toString(), "Count")
            }
        }

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)
        S0 == s0_count
        SA == SA_count
        S0_a_admin == S0_a_admin_count
        if (steps != null) {
            reposeLogSearch.searchByString(steps).size() == 1
        }

        where:
        method   | headers                                             | responseCode | steps                                                                                             | SA_count | S0_a_admin_count
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_observer\",\"d58e3_a_observer\",\"d58e4_a_observer\",\"SA\"\\]\\}" | 1        | 0
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_observer\",\"d58e3_a_observer\",\"d58e4_a_observer\",\"SA\"\\]\\}" | 2        | 0
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e4_a_observer\",\"SA\"\\]\\}"       | 3        | 1
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e4_a_observer\",\"SA\"\\]\\}"       | 4        | 2
        "GET"    | ["x-roles": "raxRolesEnabled"]                      | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 4        | 2
        "GET"    | ["x-roles": "raxRolesEnabled, a:creator"]           | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 4        | 2
        "GET"    | null                                                | "403"        | null                                                                                              | 4        | 2
        "POST"   | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e8_a_admin\",\"SA\"\\]\\}"          | 5        | 3
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e8_a_admin\",\"SA\"\\]\\}"          | 6        | 4
        "POST"   | ["x-roles": "raxRolesEnabled"]                      | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 6        | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:observer"]          | "405"        | "\\{\"steps\":\\[\"S0\",\"S0_a_observer\",\"d58e3_a_observer\",\"d58e3MF_a_observer\"\\]\\}"      | 6        | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 6        | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:observer"]   | "405"        | "\\{\"steps\":\\[\"S0\",\"S0_a_observer\",\"d58e3_a_observer\",\"d58e3MF_a_observer\"\\]\\}"      | 6        | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:creator"]           | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 6        | 4
        "POST"   | null                                                | "403"        | null                                                                                              | 6        | 4//this will not effect config change
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e11_a_admin\",\"SA\"\\]\\}"         | 7        | 5
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin, a:bar"]      | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e11_a_admin\",\"SA\"\\]\\}"         | 8        | 6
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e11_a_admin\",\"SA\"\\]\\}"         | 9        | 7
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:admin"] | "200"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e11_a_admin\",\"SA\"\\]\\}"         | 10       | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, observer, creator"]   | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "DELETE" | null                                                | "403"        | null                                                                                              | 10       | 8//this will not effect config change
        // PUT method is not available in wadl should expect to get 405 to whoever rax-role
        "PUT"    | ["x-roles": "raxRolesEnabled"]                      | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "405"        | "\\{\"steps\":\\[\"S0\",\"S0_a_observer\",\"d58e3_a_observer\",\"d58e3MF_a_observer\"\\]\\}"      | 10       | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "404"        | "\\{\"steps\":\\[\"S0\",\"d58e3UF_a_observer\"\\]\\}"                                             | 10       | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "405"        | "\\{\"steps\":\\[\"S0\",\"S0_a_admin\",\"d58e3_a_admin\",\"d58e3MF_a_admin\"\\]\\}"               | 10       | 10
    }
}
