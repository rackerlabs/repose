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
package features.filters.apivalidator.saxonee

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import scaffold.category.XmlParsing

import static javax.servlet.http.HttpServletResponse.*

/*
 * Api validator tests ported over from and JMeter
 */

@Category(XmlParsing)
class ApiValidatorSaxonEETest extends ReposeValveTest {

    private final String baseGroupPath = "/wadl/group1"
    private final String baseDefaultPath = "/wadl/default"

    private final Map<String, String> defaultHeaders = [
        "Accept"         : "application/xml",
        "Host"           : "localhost",
        "Accept-Encoding": "identity",
        "User-Agent"     : "gdeproxy"
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def saxonHome = System.getenv("SAXON_HOME")

        //If we're the jenkins user, set it, and see if it works
        if (saxonHome == null && System.getenv("LOGNAME").equals("jenkins")) {
            //For jenkins, it's going to be in $HOME/saxon_ee
            def home = System.getenv("HOME")
            saxonHome = "${home}/saxon_ee"
            repose.addToEnvironment("SAXON_HOME", saxonHome)
        }

        assert saxonHome != null

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/saxonEE", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Happy path: when no role passed, should get default wadl"() {
        setup: "declare custom handler"
        def customHandler = { return new Response(SC_OK, "OK", [], reqBody) }

        when: "When Requesting method request"
        def messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseDefaultPath$request",
            method: method,
            headers: defaultHeaders,
            requestBody: reqBody,
            defaultHandler: customHandler,
            addDefaultHeaders: false
        )

        then: "result should be responseCode"
        messageChain.receivedResponse.code as Integer == responseCode

        where:
        responseCode              | request                                               | method | reqBody
        SC_OK                     | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"  | "GET"  | ""
        SC_NOT_FOUND              | "/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" | "GET"  | ""
        SC_METHOD_NOT_ALLOWED     | "/resource1/id"                                       | "POST" | ""
        SC_UNSUPPORTED_MEDIA_TYPE | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"  | "PUT"  | "some data"
    }


    def "Happy path: when Group Passed, Should Get Role Specific WADL"() {
        setup: "declare additional headers"
        Map<String, String> headers = ["X-Roles": "group1", "Content-Type": "application/xml"]

        when: "When Requesting resource with x-roles"
        def messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseGroupPath/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            method: "GET",
            headers: defaultHeaders + headers
        )

        then: "should return resource"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "When Requesting invalid resource with x-roles"
        messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseGroupPath/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            method: "GET",
            headers: defaultHeaders + headers
        )

        then: "should return not found"
        messageChain.receivedResponse.code as Integer == SC_NOT_FOUND
        // @TODO: Bring this back in when the RMS replacement is in use.
        //messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using invalid method with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
            "/resource1/id", method: "POST", headers: defaultHeaders + headers)

        then: "should return not found"
        messageChain.receivedResponse.code as Integer == SC_METHOD_NOT_ALLOWED
        // @TODO: Bring this back in when the RMS replacement is in use.
        //messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using valid media type with x-roles"
        messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseGroupPath/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            method: "POST",
            headers: defaultHeaders + headers,
            requestBody: "<c xmlns='http://test.openrespose/test/v1.1'><test>some data</test></c>"
        )

        then: "should return OK"
        messageChain.receivedResponse.code as Integer == SC_OK
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("some data")
    }

    def "Happy path: when Ignore XSD Extension enabled"() {
        setup: "declare additional headers"
        Map<String, String> headers = ["X-Roles": "default", "Content-Type": "application/xml"]

        when: "When Requesting with valid content"
        def messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseDefaultPath/resource2/unvalidated/echobody",
            method: "PUT",
            headers: defaultHeaders + headers,
            requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>"
        )

        then: "should return resource"
        messageChain.receivedResponse.code as Integer == SC_OK
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")

        when: "When Requesting with invalid content and Ignore XSD enabled"
        messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseDefaultPath/resource2/unvalidated/echobody",
            method: "PUT",
            headers: defaultHeaders + headers,
            requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node2 id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>"
        )

        then: "should return resource"
        messageChain.receivedResponse.code as Integer == SC_OK
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node2 hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")
    }

    def "Unhappy path: When Passing to resource without required header"() {
        when: "When Requesting default resource with no roles without required header"
        def messageChain = deproxy.makeRequest(
            url: "$reposeEndpoint$baseDefaultPath/resource1/id/reqheader",
            method: "GET",
            headers: defaultHeaders
        )

        then: "should return resource"
        messageChain.receivedResponse.code as Integer == SC_BAD_REQUEST
        // @TODO: Bring this back in when the RMS replacement is in use.
        //messageChain.receivedResponse.body.contains("Expecting an HTTP header x-required-header")
        // Until then, the info is in the Message.
        messageChain.receivedResponse.message.contains("Expecting an HTTP header x-required-header")
    }
}
