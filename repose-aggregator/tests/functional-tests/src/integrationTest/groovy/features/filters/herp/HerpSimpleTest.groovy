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
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

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

    def "encoded query parameters should be decoded and logged"() {
        setup:
        String queryParamKey = "Test"
        String encodedQueryParamValue = "%21%40%23%24%25%5E%26Z%2A9%29"
        reposeLogSearch.cleanLog()

        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "?" + queryParamKey + "=" + encodedQueryParamValue
        )

        and:
        String logLine = reposeLogSearch.searchByString("INFO  highly-efficient-record-processor")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        JsonSlurper slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then:
        messageChain.receivedResponse.code == "200"

        and:
        String decodedQueryParamValue = URLDecoder.decode(encodedQueryParamValue, StandardCharsets.US_ASCII.name())
        result.Request.Parameters.Test == [decodedQueryParamValue]
    }

    @Unroll
    def "Happy path using HERP filter with method #method, origin service respCode #responseCode"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'             : 'application/xml',
                'Host'               : 'LocalHost',
                'User-agent'         : 'gdeproxy',
                'x-tenant-id'        : '123456',
                'x-roles'            : 'default',
                'x-user-name'        : 'testuser',
                'x-user-id'          : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id'  : '123456'
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

    @Unroll
    def "With API body with HERP filter with method #method, parameters #parameters, origin service respCode #responseCode"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'             : 'application/xml',
                'Host'               : 'LocalHost',
                'User-agent'         : 'gdeproxy',
                'x-tenant-id'        : '123456',
                'x-roles'            : 'default',
                'x-user-name'        : 'testuser',
                'x-user-id'          : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id'  : '123456'
        ]
        if (responseCode != "200") {
            customHandler = { return new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when:
        "When Requesting " + method + "server/abcd"
        mc = deproxy.makeRequest(url: reposeEndpoint +
                "/resource?" + parameters, method: method, headers: headers,
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

    @Unroll
    def "Test ProjectID support with HERP filter with method #method, parameters #parameters, origin service respCode #responseCode"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'             : 'application/xml',
                'Host'               : 'LocalHost',
                'User-agent'         : 'gdeproxy; all kinds of junk cause that is how we do',
                'x-project-id'       : '123456',
                'x-roles'            : 'default',
                'x-user-name'        : 'testuser',
                'x-user-id'          : 'testuser',
                'x-impersonator-name': 'impersonateuser',
                'x-impersonator-id'  : '123456'
        ]
        if (responseCode != "200") {
            customHandler = { return new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when:
        "When Requesting " + method + "server/abcd"
        mc = deproxy.makeRequest(url: reposeEndpoint +
                "/resource?" + parameters, method: method, headers: headers,
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
        result.Request.UserAgent == "gdeproxy; all kinds of junk cause that is how we do"
        result.Request.UserName == "testuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)


        where:
        responseCode | parameters               | method  | respMsg
        "200"        | "username=test"          | "POST"  | "OK"
        "200"        | "projectId=12345"        | "PUT"   | "OK"
        "415"        | "id=12345&projectId=123" | "PATCH" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "resourceId=test123"     | "PUT"   | "PAYLOAD_TOO_LARGE"
        "500"        | "id=test123&id=123"      | "PUT"   | "INTERNAL_SERVER_ERROR"
        "500"        | "name=test%20repose"     | "PUT"   | "INTERNAL_SERVER_ERROR"
    }


    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "$reposeEndpoint/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll
    def "Requests - headers: #headerName with \"#headerValue\" should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll
    def "Responses - headers: #headerName with \"#headerValue\" should keep its case in responses"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, defaultHandler: { new Response(200, null, headers) })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName     | headerValue
        "x-auth-token" | "123445"
        "X-AUTH-TOKEN" | "239853"
        "x-AUTH-token" | "slDSFslk&D"
        "x-auth-TOKEN" | "sl4hsdlg"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
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
        Map<String, List> params = [:]
        List<String> list = parameters.split("&")
        List<String> av = []
        for (e in list) {
            def (k, v) = e.split("=")
            av.add(v)
            if (params[k] == null) {
                params[k] = av
                av = []
            } else {
                List ov = params[k]
                ov.add(v)
                params[k] = ov
            }
        }
        return params
    }

    // Check if all parameters include in Parameters tag
    private boolean checkParams(String jsonpart, Map<String, List> map) {
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        boolean check = true

        for (e in map) {
            List iv = e.value
            for (v in iv) {
                if (!(result.Request.Parameters.(e.key).contains(URLDecoder.decode(v, "UTF-8")))) {
                    check = false
                    break
                }
            }
        }
        return check
    }
}
