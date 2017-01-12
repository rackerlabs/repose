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

package features.filters.samlpolicy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import static features.filters.samlpolicy.SamlPayloads.*
import static features.filters.samlpolicy.SamlUtilities.*

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED
import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE

/**
 * This functional test goes through the shared validation logic between Flow 1.0 and 2.0 including the requirements
 * for the HTTP Method, Content-Type, and base64 encoding.
 */
class SamlBasicValidationTest extends ReposeValveTest {

    def static xmlSlurper = new XmlSlurper()

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)
    }

    @Unroll
    def "a valid request will make it to the origin service and back to the client successfully with form parameters: #formParams.keySet()"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm(formParams))

        then: "the request is successfully processed"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request as valid XML"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        xmlSlurper.parseText(mc.handlings[0].request.body as String)

        where:
        formParams << [
                [(PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64],
                [(PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64, (PARAM_RELAY_STATE): "pineapple"],
                [(PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64, (PARAM_RELAY_STATE): "pineapple", (PARAM_EXTRANEOUS): "bbq"]]
    }

    @Unroll
    def "a request with Content-Type '#contentType' and a body with #bodySummary should be rejected"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): contentType],
                requestBody: requestBody)

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_UNSUPPORTED_MEDIA_TYPE

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()

        where:
        contentType          | bodySummary       | requestBody
        CONTENT_TYPE_XML     | "valid form data" | asUrlEncodedForm((PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64)
        CONTENT_TYPE_XML     | "xml"             | SAML_ONE_ASSERTION_SIGNED
        CONTENT_TYPE_INVALID | "xml"             | SAML_ONE_ASSERTION_SIGNED
    }

    def "a request using the wrong form parameter name should be rejected"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_EXTRANEOUS): SAML_ONE_ASSERTION_SIGNED_BASE64))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    def "a request without any parameters nor a request body should be rejected"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED])

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    @Unroll
    def "a request using the HTTP method #httpMethod should be rejected"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_METHOD_NOT_ALLOWED

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()

        where:
        httpMethod << HTTP_UNSUPPORTED_METHODS
    }
}
