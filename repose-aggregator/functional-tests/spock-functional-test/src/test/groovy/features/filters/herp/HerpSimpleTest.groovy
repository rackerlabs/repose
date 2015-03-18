/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package features.filters.herp
import framework.ReposeValveTest
import groovy.json.JsonSlurper
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll
/**
 * Created by jennyvo on 12/16/14.
 */
class HerpSimpleTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "simple simple test"() {
        setup:
        List listattr = ["GUI", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        reposeLogSearch.cleanLog()
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: ['Accept': 'application/xml'])
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        messageChain.receivedResponse.code == "200"
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.Method == "GET"
        result.Response.Code == 200
        result.Response.Message == "OK"
    }

    @Unroll("Test Herp filter with method #method, origin service respCode #responseCode")
    def "Happy path using herp with simple request"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'           : 'application/xml',
                'Host'             : 'LocalHost',
                'User-agent'       : 'gdeproxy',
                'x-tenant-id'      : '123456',
                'x-roles'          : 'default',
                'x-user-name'      : 'testuser',
                'x-user-id'        : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id': '123456'
        ]
        def customHandler = { return new Response(responseCode, "Resource Not Fount", [], reqBody) }

        when:
        "When Requesting " + method + " " + request
        mc = deproxy.makeRequest(url: reposeEndpoint +
                request, method: method, headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.Method == method
        (result.Request.URL).contains(request)
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg

        where:
        responseCode | request                                              | method  | reqBody     | respMsg
        "404"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" | "GET"   | ""          | "NOT_FOUND"
        "404"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-bbbbbbbbbbbb" | "GET"   | ""          | "NOT_FOUND"
        "405"        | "/resource1/id"                                      | "POST"  | ""          | "METHOD_NOT_ALLOWED"
        "400"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-cccccccccccc" | "PUT"   | "some data" | "BAD_REQUEST"
        "415"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-dddddddddddd" | "PATCH" | "some data" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-eeeeeeeeeeee" | "PUT"   | "some data" | "PAYLOAD_TOO_LARGE"
        "500"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-ffffffffffff" | "PUT"   | "some data" | "INTERNAL_SERVER_ERROR"
    }

    @Unroll("Test Herp filter with method #method, parameters #parameters, origin service respCode #responseCode")
    def "Herp test with api body request"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'           : 'application/xml',
                'Host'             : 'LocalHost',
                'User-agent'       : 'gdeproxy',
                'x-tenant-id'      : '123456',
                'x-roles'          : 'default',
                'x-user-name'      : 'testuser',
                'x-user-id'        : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id': '123456'
        ]
        if (responseCode != "200"){
            customHandler = { return new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when:
        "When Requesting " + method + "server/abcd"
        mc = deproxy.makeRequest(url: reposeEndpoint +
                "/resource?"+parameters, method: method, headers: headers,
                requestBody: "some data", defaultHandler: customHandler,
                addDefaultHeaders: false
        )
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def map = buildParamList(parameters)

        then:
        "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.ProjectID[0] == "123456"
        result.Request.UserName == "testuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)


        where:
        responseCode | parameters              | method  | respMsg
        "200"        | "username=test"         | "POST"  | "OK"
        "200"        | "tenantId=12345"        | "PUT"   | "OK"
        "415"        | "id=12345&tenandId=123" | "PATCH" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "resourceId=test123"    | "PUT"   | "PAYLOAD_TOO_LARGE"
        "500"        | "id=test123&id=123"     | "PUT"   | "INTERNAL_SERVER_ERROR"
        "500"        | "name=test%20repose"    | "PUT"   | "INTERNAL_SERVER_ERROR"
    }

    @Unroll("Test Herp filter with method #method, parameters #parameters, origin service respCode #responseCode")
    def "Herp test also support projectId"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'           : 'application/xml',
                'Host'             : 'LocalHost',
                'User-agent'       : 'gdeproxy',
                'x-project-id'     : '123456',
                'x-roles'          : 'default',
                'x-user-name'      : 'testuser',
                'x-user-id'        : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id': '123456'
        ]
        if (responseCode != "200"){
            customHandler = { return new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when:
        "When Requesting " + method + "server/abcd"
        mc = deproxy.makeRequest(url: reposeEndpoint +
                "/resource?"+parameters, method: method, headers: headers,
                requestBody: "some data", defaultHandler: customHandler,
                addDefaultHeaders: false
        )
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def map = buildParamList(parameters)

        then:
        "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.ProjectID[0] == "123456"
        result.Request.UserName == "testuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)


        where:
        responseCode | parameters              | method  | respMsg
        "200"        | "username=test"         | "POST"  | "OK"
        "200"        | "projectId=12345"       | "PUT"   | "OK"
        "415"        | "id=12345&projectId=123"| "PATCH" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "resourceId=test123"    | "PUT"   | "PAYLOAD_TOO_LARGE"
        "500"        | "id=test123&id=123"     | "PUT"   | "INTERNAL_SERVER_ERROR"
        "500"        | "name=test%20repose"    | "PUT"   | "INTERNAL_SERVER_ERROR"
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

    // Build map for query parameters from request
    private Map<String, List> buildParamList(String parameters) {
        Map <String, List> params = [:]
        List <String> list = parameters.split("&")
        List <String> av = []
        for (e in list) {
            def (k, v) = e.split("=")
            av.add(v)
            if (params[k]==null) {
                params[k] = av
                av = []
            }
            else {
                List ov = params[k]
                ov.add(v)
                params[k] = ov
            }
        }
        return params
    }

    // Check if all parameters include in Parameters tag
    private boolean checkParams(String jsonpart, Map <String, List> map) {
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        boolean check = true

        for (e in map) {
            List iv = e.value
            for (v in iv) {
                if (!(result.Request.Parameters.(e.key).contains(URLDecoder.decode(v,"UTF-8")))) {
                    check = false
                    break
                }
            }
        }
        return check
    }
}