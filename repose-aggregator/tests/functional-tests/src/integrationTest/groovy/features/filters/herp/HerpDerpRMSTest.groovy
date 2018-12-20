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
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 6/15/15.
 * Test Herp Derp filter with response message config
 */
@Ignore
class HerpDerpRMSTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/herp", params)
        repose.configurationProvider.applyConfigs("features/filters/herp/wderpandrms", params)
        repose.start()
    }

    def static params

    @Unroll("req method: #method, #path, #roles")
    def "when req without token, non tenanted and delegable mode (2) with quality"() {
        given:
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message", "MethodLabel"]

        Map<String, String> headers = ["X-Roles"     : roles,
                                       "Content-Type": "application/xml"]
        reposeLogSearch.cleanLog()

        when: "User passes a request through repose with authN and apiValidator delegable"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/$path",
                method: method,
                headers: headers)

        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then: "Request body sent from repose to the origin service should contain"
        checkAttribute(jsonpart, listattr)
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("Content-Type")
        mc.receivedResponse.headers.getFirstValue("Content-Type") != "text/plain"
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0


        where:
        method   | path           | roles                         | responseCode | msgBody              | component       | quality
        "GET"    | "servers/"     | "raxRolesEnabled"             | "403"        | "Forbidden"          | "api-validator" | 0.6
        "POST"   | "servers/1235" | "raxRolesEnabled, a:observer" | "404"        | "Resource not found" | "api-validator" | 0.6
        "PUT"    | "servers/"     | "raxRolesEnabled, a:admin"    | "405"        | "Method not allowed" | "api-validator" | 0.6
        "DELETE" | "servers/test" | "raxRolesEnabled, a:observer" | "404"        | "Resource not found" | "api-validator" | 0.6
        "GET"    | "get/"         | "raxRolesEnabled"             | "404"        | "Resource not found" | "api-validator" | 0.6

    }


    @Unroll("method=#method,headers=#headers")
    def "when enable-api-coverage is true, validate count at state level"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message", "MethodLabel"]
        reposeLogSearch.cleanLog()

        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/servers", method: method, headers: headers,
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
