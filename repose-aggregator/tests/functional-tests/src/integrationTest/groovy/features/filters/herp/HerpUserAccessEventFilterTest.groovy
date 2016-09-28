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

import framework.ReposeValveTest
import groovy.json.JsonSlurper
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 2/10/15.
 */
class HerpUserAccessEventFilterTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.configurationProvider.applyConfigs('features/filters/herp/userAccessEventFilter', params)
        repose.start()
    }

    @Unroll("Test filterout for Herp with method #method, username #username and origin service respCode #responseCode")
    def "Events match filterout condition will not go to post filter log"() {
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
                'x-user-name'        : username,
                'x-user-id'          : username,
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
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
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
        result.Request.ProjectID[0] == "123456"
        result.Request.UserName == username
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains(request)
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        //all condition match support filterout event and not going to post filter log
        reposeLogSearch.searchByString("INFO  org.openrepose.herp.post.filter").size() == 0

        where:
        responseCode | username      | request                      | method  | reqBody     | respMsg
        "404"        | "User"        | "/resource1/id/aaaaaaaaaaaa" | "GET"   | ""          | "NOT_FOUND"
        "405"        | "testUser"    | "/resource1/id"              | "POST"  | ""          | "METHOD_NOT_ALLOWED"
        "400"        | "reposeUser"  | "/resource1/id/cccccccccccc" | "PUT"   | "some data" | "BAD_REQUEST"
        "415"        | "reposeUser1" | "/resource1/id/dddddddddddd" | "PATCH" | "some data" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "reposeTest"  | "/resource1/id/eeeeeeeeeeee" | "PUT"   | "some data" | "PAYLOAD_TOO_LARGE"
        "500"        | "reposeTest1" | "/resource1/id/ffffffffffff" | "PUT"   | "some data" | "INTERNAL_SERVER_ERROR"
    }

    @Unroll("Test not match condition from filterout: method #method, tenantId #tenantid, parameters #parameters, origin service respCode #responseCode")
    def "Events that not match the condition with find in post filter log"() {
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
                'x-tenant-id'        : tenantid,
                'x-roles'            : 'default',
                'x-user-name'        : 'randomuser',
                'x-user-id'          : 'randomuser',
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
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def map = buildParamList(parameters)
        String postFilterLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.post.filter")
        String pjsonpart = postFilterLine.substring(postFilterLine.indexOf("{"))
        def pslurper = new JsonSlurper()
        def presult = pslurper.parseText(pjsonpart)
        def pmap = buildParamList(parameters)

        then:
        "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.ProjectID[0] == tenantid
        result.Request.UserName == "randomuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)
        //Check post filter event log
        checkAttribute(pjsonpart, listattr)
        presult.ServiceCode == "repose"
        presult.Region == "USA"
        presult.DataCenter == "DFW"
        presult.Request.ProjectID[0] == tenantid
        presult.Request.UserName == "randomuser"
        presult.Request.ImpersonatorName == "impersonateuser"
        presult.Request.Method == method
        (presult.Request.URL).contains("/resource")
        presult.Response.Code == responseCode.toInteger()
        presult.Response.Message == respMsg
        checkParams(pjsonpart, pmap)

        where:
        responseCode | tenantid | parameters              | method  | respMsg
        "200"        | "123456" | "username=test"         | "POST"  | "OK"
        "200"        | "test12" | "tenantId=12345"        | "PUT"   | "OK"
        "415"        | "000123" | "id=12345&tenandId=123" | "PATCH" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "repose" | "resourceId=test123"    | "PUT"   | "PAYLOAD_TOO_LARGE"
        "500"        | "123456" | "id=test123&id=123"     | "PUT"   | "INTERNAL_SERVER_ERROR"
        "500"        | "123456" | "name=test%20repose"    | "PUT"   | "INTERNAL_SERVER_ERROR"
    }

    @Unroll("Filter also work for projectId and OR condition: #username, #method, #projectid, #parameters, origin service respCode #responseCode")
    def "Filter also work for projectId and OR condition"() {
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
                'x-project-id'       : projectid,
                'x-roles'            : 'default',
                'x-user-name'        : username,
                'x-user-id'          : username,
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
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
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
        result.Request.ProjectID[0] == projectid
        result.Request.UserName == username
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)
        reposeLogSearch.searchByString("INFO  org.openrepose.herp.post.filter").size() == 0


        where:
        responseCode | username   | projectid | parameters              | method  | respMsg
        "200"        | "User"     | "123456"  | "username=test"         | "POST"  | "OK"
        "200"        | "admin"    | "-123456" | "tenantId=12345"        | "PUT"   | "OK"
        "415"        | "repose"   | "-123456" | "id=12345&tenandId=123" | "PATCH" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "testUser" | "123456"  | "resourceId=test123"    | "PUT"   | "PAYLOAD_TOO_LARGE"
        "500"        | "Test"     | "000456"  | "id=test123&id=123"     | "PUT"   | "INTERNAL_SERVER_ERROR"
        "500"        | "test"     | "-123456" | "name=test%20repose"    | "PUT"   | "INTERNAL_SERVER_ERROR"
        "200"        | "admin"    | "123456"  | "tenantId=-123456"      | "POST"  | "OK"
    }

    @Unroll("Tracing header test with #method, #tenantid, #parameters")
    def "Tracing header test with user access event"() {
        setup: "declare messageChain to be of type MessageChain"
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "RequestID", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        reposeLogSearch.cleanLog()
        MessageChain mc
        def Map<String, String> headers = [
                'Accept'             : 'application/xml',
                'Host'               : 'LocalHost',
                'User-agent'         : 'gdeproxy',
                'x-tenant-id'        : tenantid,
                'x-roles'            : 'default',
                'x-user-name'        : 'randomuser',
                'x-user-id'          : 'randomuser',
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
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def map = buildParamList(parameters)
        String postFilterLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.post.filter")
        String pjsonpart = postFilterLine.substring(postFilterLine.indexOf("{"))
        def pslurper = new JsonSlurper()
        def presult = pslurper.parseText(pjsonpart)
        def pmap = buildParamList(parameters)
        def requestid = TracingHeaderHelper.getTraceGuid(mc.handlings[0].request.headers.getFirstValue("x-trans-id"))

        then:
        "result should be " + responseCode
        mc.receivedResponse.code.equals(responseCode)
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.ProjectID[0] == tenantid
        result.Request.UserName == "randomuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.RequestID == requestid
        result.Request.Method == method
        (result.Request.URL).contains("/resource")
        result.Response.Code == responseCode.toInteger()
        result.Response.Message == respMsg
        checkParams(jsonpart, map)
        //Check post filter event log
        checkAttribute(pjsonpart, listattr)
        presult.ServiceCode == "repose"
        presult.Region == "USA"
        presult.DataCenter == "DFW"
        presult.Request.ProjectID[0] == tenantid
        presult.Request.UserName == "randomuser"
        presult.Request.ImpersonatorName == "impersonateuser"
        presult.Request.RequestID == requestid
        presult.Request.Method == method
        (presult.Request.URL).contains("/resource")
        presult.Response.Code == responseCode.toInteger()
        presult.Response.Message == respMsg
        checkParams(pjsonpart, pmap)


        where:
        responseCode | tenantid | parameters       | method | respMsg
        "200"        | "123456" | "username=test"  | "POST" | "OK"
        "200"        | "test12" | "tenantId=12345" | "PUT"  | "OK"
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
