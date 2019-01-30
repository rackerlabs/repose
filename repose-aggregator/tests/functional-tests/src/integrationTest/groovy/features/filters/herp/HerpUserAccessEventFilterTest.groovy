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
import org.openrepose.commons.utils.http.OpenStackServiceHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

import static javax.ws.rs.HttpMethod.GET

@Category(Filters)
class HerpUserAccessEventFilterTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs('features/filters/herp', params)
        repose.configurationProvider.applyConfigs('features/filters/herp/userAccessEventFilter', params)
        repose.start()
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    @Unroll("Test filterout for Herp with method #method, username #username and origin service respCode #responseCode")
    def "Events match filterout condition will not go to post filter log"() {
        given:
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]

        Map<String, String> headers = [
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
        def customHandler = { new Response(responseCode, "Resource Not Fount", [], reqBody) }

        when: "requesting endpoint #request with method #method"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + request,
                method: method,
                headers: headers,
                requestBody: reqBody,
                defaultHandler: customHandler,
                addDefaultHeaders: false)
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)

        then: "result should be #responseCode"
        mc.receivedResponse.code == responseCode
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

        and: "all condition match support filterout event and not going to post filter log"
        reposeLogSearch.searchByString("INFO  org.openrepose.herp.post.filter").isEmpty()

        where:
        responseCode | username      | request                      | method  | reqBody     | respMsg
        "404"        | "User"        | "/resource1/id/aaaaaaaaaaaa" | "GET"   | ""          | "NOT_FOUND"
        "405"        | "testUser"    | "/resource1/id"              | "POST"  | ""          | "METHOD_NOT_ALLOWED"
        "400"        | "reposeUser"  | "/resource1/id/cccccccccccc" | "PUT"   | "some data" | "BAD_REQUEST"
        "415"        | "reposeUser1" | "/resource1/id/dddddddddddd" | "PATCH" | "some data" | "UNSUPPORTED_MEDIA_TYPE"
        "413"        | "reposeTest"  | "/resource1/id/eeeeeeeeeeee" | "PUT"   | "some data" | "PAYLOAD_TOO_LARGE"
        "500"        | "reposeTest1" | "/resource1/id/ffffffffffff" | "PUT"   | "some data" | "INTERNAL_SERVER_ERROR"
    }

    @Unroll("Tenant IDs of '#requestTenantIds' in the request and '#responseTenantIds' in the response should result in DefaultProjectID of '#defaultProject' and ProjectID of '#projectIds' in the logged JSON")
    def "Tenant ID test"() {
        given:
        def requestHeaders = requestTenantIds.collect { new Header(OpenStackServiceHeader.TENANT_ID, it) }
        def responseHeaders = responseTenantIds.collect { new Header(OpenStackServiceHeader.TENANT_ID, it) }
        def customHandler = { new Response(HttpServletResponse.SC_NOT_IMPLEMENTED, null, responseHeaders) }

        when: "making the request with headers #requestHeaders"
        deproxy.makeRequest(
                url: reposeEndpoint,
                method: GET,
                headers: requestHeaders,
                defaultHandler: customHandler)

        then: "the filter will log the details of the request using JSON"
        def logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter").first()
        def result = new JsonSlurper().parseText(logLine.substring(logLine.indexOf("{")))

        and: "the logged JSON will contain the expected Default Project ID and list of Project IDs"
        result.Request.DefaultProjectID == defaultProject
        result.Request.ProjectID == projectIds

        where:
        requestTenantIds                               | responseTenantIds                              || defaultProject | projectIds
        []                                             | []                                             || ''             | []
        ['foo']                                        | []                                             || 'foo'          | ['foo']
        ['foo', 'bar']                                 | []                                             || 'foo'          | ['foo', 'bar']
        // if the tenant ID isn't available in the request, it should use the value in the response
        []                                             | ['foo']                                        || 'foo'          | ['foo']
        []                                             | ['foo', 'bar']                                 || 'foo'          | ['foo', 'bar']
        // DefaultProjectID should be populated with highest quality header
        ['foo;q=0.5', 'bar;q=1.0']                     | []                                             || 'bar'          | ['foo', 'bar']
        ['foo;q=0.5,bar;q=1.0']                        | []                                             || 'bar'          | ['foo', 'bar']
        []                                             | ['foo;q=0.5', 'bar;q=1.0']                     || 'bar'          | ['foo', 'bar']
        []                                             | ['foo;q=0.5,bar;q=1.0']                        || 'bar'          | ['foo', 'bar']
        // header quality should default to 1.0 if it's not specified
        ['foo', 'bar;q=0.9']                           | []                                             || 'foo'          | ['foo', 'bar']
        ['bar;q=0.9', 'foo']                           | []                                             || 'foo'          | ['bar', 'foo']
        ['foo,bar;q=0.9']                              | []                                             || 'foo'          | ['foo', 'bar']
        ['bar;q=0.9,foo']                              | []                                             || 'foo'          | ['bar', 'foo']
        []                                             | ['foo', 'bar;q=0.9']                           || 'foo'          | ['foo', 'bar']
        []                                             | ['bar;q=0.9', 'foo']                           || 'foo'          | ['bar', 'foo']
        []                                             | ['foo,bar;q=0.9']                              || 'foo'          | ['foo', 'bar']
        []                                             | ['bar;q=0.9,foo']                              || 'foo'          | ['bar', 'foo']
        // headers should be properly split with the quality used to determine the DefaultProjectID
        ['foo;q=1.0,bar;q=0.7', 'baz;q=0.3']           | []                                             || 'foo'          | ['foo', 'bar', 'baz']
        ['foo;q=0.7,bar;q=1.0', 'baz;q=0.3']           | []                                             || 'bar'          | ['foo', 'bar', 'baz']
        ['foo;q=0.3,bar;q=0.7', 'baz;q=1.0']           | []                                             || 'baz'          | ['foo', 'bar', 'baz']
        ['foo,bar;q=0.7', 'baz;q=0.3']                 | []                                             || 'foo'          | ['foo', 'bar', 'baz']
        ['foo;q=0.7,bar', 'baz;q=0.3']                 | []                                             || 'bar'          | ['foo', 'bar', 'baz']
        ['foo;q=0.3,bar;q=0.7', 'baz']                 | []                                             || 'baz'          | ['foo', 'bar', 'baz']
        ['foo;q=1.0,bar;q=0.7', 'baz;q=0.3,qux;q=0.4'] | []                                             || 'foo'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar;q=1.0', 'baz;q=0.3,qux;q=0.4'] | []                                             || 'bar'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar;q=0.3', 'baz;q=1.0,qux;q=0.4'] | []                                             || 'baz'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar;q=0.4', 'baz;q=0.3,qux;q=1.0'] | []                                             || 'qux'          | ['foo', 'bar', 'baz', 'qux']
        ['foo,bar;q=0.7', 'baz;q=0.3,qux;q=0.4']       | []                                             || 'foo'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar', 'baz;q=0.3,qux;q=0.4']       | []                                             || 'bar'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar;q=0.3', 'baz,qux;q=0.4']       | []                                             || 'baz'          | ['foo', 'bar', 'baz', 'qux']
        ['foo;q=0.7,bar;q=0.4', 'baz;q=0.3,qux']       | []                                             || 'qux'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=1.0,bar;q=0.7', 'baz;q=0.3']           || 'foo'          | ['foo', 'bar', 'baz']
        []                                             | ['foo;q=0.7,bar;q=1.0', 'baz;q=0.3']           || 'bar'          | ['foo', 'bar', 'baz']
        []                                             | ['foo;q=0.3,bar;q=0.7', 'baz;q=1.0']           || 'baz'          | ['foo', 'bar', 'baz']
        []                                             | ['foo,bar;q=0.7', 'baz;q=0.3']                 || 'foo'          | ['foo', 'bar', 'baz']
        []                                             | ['foo;q=0.7,bar', 'baz;q=0.3']                 || 'bar'          | ['foo', 'bar', 'baz']
        []                                             | ['foo;q=0.3,bar;q=0.7', 'baz']                 || 'baz'          | ['foo', 'bar', 'baz']
        []                                             | ['foo;q=1.0,bar;q=0.7', 'baz;q=0.3,qux;q=0.4'] || 'foo'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar;q=1.0', 'baz;q=0.3,qux;q=0.4'] || 'bar'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar;q=0.3', 'baz;q=1.0,qux;q=0.4'] || 'baz'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar;q=0.4', 'baz;q=0.3,qux;q=1.0'] || 'qux'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo,bar;q=0.7', 'baz;q=0.3,qux;q=0.4']       || 'foo'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar', 'baz;q=0.3,qux;q=0.4']       || 'bar'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar;q=0.3', 'baz,qux;q=0.4']       || 'baz'          | ['foo', 'bar', 'baz', 'qux']
        []                                             | ['foo;q=0.7,bar;q=0.4', 'baz;q=0.3,qux']       || 'qux'          | ['foo', 'bar', 'baz', 'qux']
        // header attributes should be properly parsed - numbers of digits
        ['foo;q=0.1,bar;q=0.5']                        | []                                             || 'bar'          | ['foo', 'bar']
        ['foo;q=0.01,bar;q=0.5']                       | []                                             || 'bar'          | ['foo', 'bar']
        ['foo;q=0.001,bar;q=0.5']                      | []                                             || 'bar'          | ['foo', 'bar']
        ['foo;q=1,bar;q=0.5']                          | []                                             || 'foo'          | ['foo', 'bar']
        ['foo;q=1.,bar;q=0.5']                         | []                                             || 'foo'          | ['foo', 'bar']
        ['foo;q=1.0,bar;q=0.5']                        | []                                             || 'foo'          | ['foo', 'bar']
        ['foo;q=1.00,bar;q=0.5']                       | []                                             || 'foo'          | ['foo', 'bar']
        ['foo;q=1.000,bar;q=0.5']                      | []                                             || 'foo'          | ['foo', 'bar']
        []                                             | ['foo;q=0.1,bar;q=0.5']                        || 'bar'          | ['foo', 'bar']
        []                                             | ['foo;q=0.01,bar;q=0.5']                       || 'bar'          | ['foo', 'bar']
        []                                             | ['foo;q=0.001,bar;q=0.5']                      || 'bar'          | ['foo', 'bar']
        []                                             | ['foo;q=1,bar;q=0.5']                          || 'foo'          | ['foo', 'bar']
        []                                             | ['foo;q=1.,bar;q=0.5']                         || 'foo'          | ['foo', 'bar']
        []                                             | ['foo;q=1.0,bar;q=0.5']                        || 'foo'          | ['foo', 'bar']
        []                                             | ['foo;q=1.00,bar;q=0.5']                       || 'foo'          | ['foo', 'bar']
        []                                             | ['foo;q=1.000,bar;q=0.5']                      || 'foo'          | ['foo', 'bar']
        // header attributes should be properly parsed - other header attributes should be ignored
        ['foo;q=1.0;qe=hi']                            | []                                             || 'foo'          | ['foo']
        ['foo;qe=hi;q=1.0']                            | []                                             || 'foo'          | ['foo']
        ['foo;q=1.0;eq=hi']                            | []                                             || 'foo'          | ['foo']
        ['foo;eq=hi;q=1.0']                            | []                                             || 'foo'          | ['foo']
        ['foo;q=1.0;qq=hi']                            | []                                             || 'foo'          | ['foo']
        ['foo;qq=hi;q=1.0']                            | []                                             || 'foo'          | ['foo']
        ['low;qq=1.0;q=0.2,high;qq=0.3;q=0.8']         | []                                             || 'high'         | ['low', 'high']
        []                                             | ['foo;q=1.0;qe=hi']                            || 'foo'          | ['foo']
        []                                             | ['foo;qe=hi;q=1.0']                            || 'foo'          | ['foo']
        []                                             | ['foo;q=1.0;eq=hi']                            || 'foo'          | ['foo']
        []                                             | ['foo;eq=hi;q=1.0']                            || 'foo'          | ['foo']
        []                                             | ['foo;q=1.0;qq=hi']                            || 'foo'          | ['foo']
        []                                             | ['foo;qq=hi;q=1.0']                            || 'foo'          | ['foo']
        []                                             | ['low;qq=1.0;q=0.2,high;qq=0.3;q=0.8']         || 'high'         | ['low', 'high']
        // the Tenant ID in the request should always take precedence over the response
        ['requestTnt']                                 | ['responseTnt']                                || 'requestTnt'   | ['requestTnt']
        ['requestTnt;foo=bar']                         | ['responseTnt;foo=bar']                        || 'requestTnt'   | ['requestTnt']
        ['requestTnt;q=0.1']                           | ['responseTnt;q=1.0']                          || 'requestTnt'   | ['requestTnt']
        ['reqFoo;q=0.5', 'reqBar;q=1.0']               | ['resFoo;q=0.5', 'resBar;q=1.0']               || 'reqBar'       | ['reqFoo', 'reqBar']
        ['reqFoo;q=0.5,reqBar;q=1.0']                  | ['resFoo;q=0.5,resBar;q=1.0']                  || 'reqBar'       | ['reqFoo', 'reqBar']
    }

    @Unroll("Test not match condition from filterout: method #method, tenantId #tenantid, parameters #parameters, origin service respCode #responseCode")
    def "Events that not match the condition with find in post filter log"() {
        given:
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        Map<String, String> headers = [
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
            customHandler = { new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when: "requesting endpoint /resource with method #method and parameters #parameters"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/resource?" + parameters,
                method: method,
                headers: headers,
                requestBody: "some data",
                defaultHandler: customHandler,
                addDefaultHeaders: false)
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

        then: "result should be #responseCode"
        mc.receivedResponse.code == responseCode
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
        given:
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        Map<String, String> headers = [
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
            customHandler = { new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when: "requesting endpoint /resource with method #method and parameters #parameters"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/resource?" + parameters,
                method: method,
                headers: headers,
                requestBody: "some data",
                defaultHandler: customHandler,
                addDefaultHeaders: false)
        String logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter")
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        def map = buildParamList(parameters)

        then: "result should be #responseCode"
        mc.receivedResponse.code == responseCode
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
        given:
        List listattr = ["GUID", "ServiceCode", "Region", "DataCenter", "Timestamp", "Request", "RequestID", "Method", "URL", "Parameters",
                         "UserName", "ImpersonatorName", "ProjectID", "Role", "UserAgent", "Response", "Code", "Message"]
        def customHandler = ""

        Map<String, String> headers = [
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
            customHandler = { new Response(responseCode, "Resource Not Fount", [], "some data") }
        }

        when: "requesting endpoint /resource with method #method and parameters #parameters"
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/resource?" + parameters,
                method: method,
                headers: headers,
                requestBody: "some data",
                defaultHandler: customHandler,
                addDefaultHeaders: false)
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

        then: "result should be #responseCode"
        mc.receivedResponse.code == responseCode
        checkAttribute(jsonpart, listattr)
        result.ServiceCode == "repose"
        result.Region == "USA"
        result.DataCenter == "DFW"
        result.Request.ProjectID[0] == tenantid
        result.Request.UserName == "randomuser"
        result.Request.ImpersonatorName == "impersonateuser"
        result.Request.RequestID.tokenize(':')[0] == requestid
        result.Request.RequestID.tokenize(':')[1].length() > 0
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
        presult.Request.RequestID.tokenize(':')[0] == requestid
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

    @Unroll
    def "will populate and log the username '#expectedUsername' when the request header username is '#requestUsername' and the response header is '#responseUsername'"() {
        given:
        def requestHeaders = requestUsername != null ? [(OpenStackServiceHeader.USER_NAME): requestUsername] : [:]
        def responseHeaders = responseUsername != null ? [(OpenStackServiceHeader.USER_NAME): responseUsername] : [:]
        def customHandler = { new Response(HttpServletResponse.SC_OK, null, responseHeaders) }

        when:
        deproxy.makeRequest(
                url: reposeEndpoint,
                method: GET,
                headers: requestHeaders,
                defaultHandler: customHandler)

        then: "the filter will log the details of the request using JSON"
        def logLine = reposeLogSearch.searchByString("INFO  org.openrepose.herp.pre.filter").first()
        def result = new JsonSlurper().parseText(logLine.substring(logLine.indexOf("{")))

        and: "the logged JSON will contain the expected Username"
        result.Request.UserName == expectedUsername

        where:
        expectedUsername | requestUsername     | responseUsername
        "requestFoo"     | "requestFoo"        | null
        // should use the response header if the request header is missing or empty
        "responseBar"    | null                | "responseBar"
        "responseBar"    | ""                  | "responseBar"
        "responseBar"    | ";q=0.8"            | "responseBar"
        // should prefer the request header if it's available, no matter the quality
        "requestFoo"     | "requestFoo"        | "responseBar"
        "requestFoo"     | "requestFoo;q=0.2"  | "responseBar;q=0.9"
        "requestFoo"     | "requestFoo;q=1.0"  | "responseBar"
        "requestFoo"     | "requestFoo"        | "responseBar;q=1.0"
        "requestFoo"     | "requestFoo;q=1.0"  | "responseBar;q=1.0"
        // should not be tripped up by different header attributes
        "foo"            | "foo;q=0.4;a=b"     | null
        "foo"            | "foo;a=b;q=0.4"     | null
        "foo"            | "foo;a=b;q=0.4;c=d" | null
        "foo"            | "foo;q=0.4;qq=0.0"  | null
        "foo"            | "foo;qq=0.0;q=0.4"  | null
        "bar"            | null                | "bar;q=0.4;a=b"
        "bar"            | null                | "bar;a=b;q=0.4"
        "bar"            | null                | "bar;a=b;q=0.4;c=d"
        "bar"            | null                | "bar;q=0.4;qq=0.0"
        "bar"            | null                | "bar;qq=0.0;q=0.4"
        "baz"            | "baz;q=0.4;a=b"     | "qux;q=0.4;a=b"
        "baz"            | "baz;a=b;q=0.4"     | "qux;a=b;q=0.4"
        "baz"            | "baz;a=b;q=0.4;c=d" | "qux;a=b;q=0.4;c=d"
        "baz"            | "baz;q=0.4;qq=0.0"  | "qux;q=0.4;qq=0.0"
        "baz"            | "baz;qq=0.0;q=0.4"  | "qux;qq=0.0;q=0.4"
        // should be okay with different precision qualities
        "foo"            | "foo;q=0.1"         | null
        "foo"            | "foo;q=0.01"        | null
        "foo"            | "foo;q=0.001"       | null
        "foo"            | "foo;q=1"           | null
        "foo"            | "foo;q=1.0"         | null
        "foo"            | "foo;q=1.00"        | null
        "foo"            | "foo;q=1.000"       | null
        "bar"            | null                | "bar;q=0.1"
        "bar"            | null                | "bar;q=0.01"
        "bar"            | null                | "bar;q=0.001"
        "bar"            | null                | "bar;q=1"
        "bar"            | null                | "bar;q=1.0"
        "bar"            | null                | "bar;q=1.00"
        "bar"            | null                | "bar;q=1.000"
        // should be okay with some whitespace
        "foo"            | "foo; q=0.5"        | null
        "bar"            | null                | "bar; q=0.5"
        // should be okay with the same value
        "foo"            | "foo"               | "foo"
    }

    // Check all required attributes in the log
    private static boolean checkAttribute(String jsonpart, List<String> attributes) {
        attributes.every { jsonpart.contains(it) }
    }

    // Build map for query parameters from request
    private static Map<String, List> buildParamList(String parameterString) {
        Map<String, List> parameters = [:]
        List<String> av = []

        parameterString.split("&").each { e ->
            def (k, v) = e.split("=")
            av.add(v)
            if (parameters[k] == null) {
                parameters[k] = av
                av = []
            } else {
                parameters[k] += v
            }
        }

        parameters
    }

    // Check if all parameters include in Parameters tag
    private static boolean checkParams(String jsonpart, Map<String, List<String>> parameters) {
        def result = new JsonSlurper().parseText(jsonpart)

        parameters.every { key, values ->
            def jsonValue = result.Request.Parameters[key]
            values.every { jsonValue.contains(URLDecoder.decode(it, "UTF-8")) }
        }
    }
}
