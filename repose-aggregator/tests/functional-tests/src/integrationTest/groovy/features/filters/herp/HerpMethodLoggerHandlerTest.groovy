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
package features.filters.herp

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 2/3/15.
 *  When using with api validator with enable-api-coverage
 *  method name should be moved forward to log
 */
@Category(Filters)
class HerpMethodLoggerHandlerTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/apivalidatorstatemachine", params)
        repose.start()
    }

    def static params

    /*
        When enable-api-coverage is set to true, enable-rax-role is set to true,
        certain user roles will allow to access certain methods according to config in the wadl.
        i.e. 'GET' method only be available to access by a:observer and a:admin role
    */

    @Unroll("method=#method,headers=#headers,expected S0_a_admin:#S0_a_admin_count, SA:#SA_count")
    def "when enable-api-coverage is true, validate count at state level"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message", "MethodLabel"]
        reposeLogSearch.cleanLog()

        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/resources", method: method, headers: headers,
                defaultHandler: { new Response(200, null, null, null) })
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        checkAttribute(jsonpart, listattr)
        result.MethodLabel == label
        result.Request.Method == method

        where:
        method   | label         | headers
        "GET"    | 'Get Test'    | ["x-roles": "raxRolesEnabled, a:observer"]
        "GET"    | 'Get Test'    | ["x-roles": "raxRolesEnabled"]
        "POST"   | 'Create Test' | ["x-roles": "raxRolesEnabled, a:admin"]
        "POST"   | 'Create Test' | []
        "DELETE" | 'Delete Test' | []
    }

    // Check all required attributes in the log
    private boolean checkAttribute(String jsonpart, List listattr) {
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        boolean check = true
        for (attr in listattr) {
            if (!jsonpart.contains(attr)) {
                check = false
                break
            }
        }
        return check
    }
}
