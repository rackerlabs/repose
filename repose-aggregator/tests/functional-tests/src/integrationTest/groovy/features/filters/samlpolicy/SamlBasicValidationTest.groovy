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
import org.opensaml.saml.saml2.core.Response
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

    static xmlSlurper = new XmlSlurper()
    static samlUtilities = new SamlUtilities()

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
                method: httpMethod,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): SAML_ONE_ASSERTION_SIGNED_BASE64))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_METHOD_NOT_ALLOWED

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()

        where:
        httpMethod << HTTP_UNSUPPORTED_METHODS
    }

    @Unroll
    def "a request will be rejected when the SAMLResponse contents are invalid due to #reason"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): paramValue))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()

        where:
        reason                    | paramValue
        "invalid base64 encoding" | SAML_ONE_ASSERTION_SIGNED_BASE64 + "!@#%^*)*)@"
        "invalid XML"             | encodeBase64("legit saml response kthxbai")
        "invalid SAML"            | encodeBase64("<banana/>")
        "missing Issuer element"  | encodeBase64(samlResponse(status() >> assertion()))
        "empty Issuer element"    | encodeBase64(samlResponse({ 'saml2:Issuer'("") } >> status() >> assertion()))
    }

    // todo: fix name of test
    @Unroll
    def "verify the test SAML Utility can generate a valid SAML Response #testNum"() {
        when: "we unmarshall the SAML string and try to validate the first Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the signature was valid"
        isValidSignature

        and: "the Issuer was set correctly"
        response.getIssuer().value == "http://idp.external.com"

        where:
        [testNum, saml] << [
                [0, samlResponse {
                    'saml2:Issuer'("http://idp.external.com")
                    'saml2p:Status' {
                        'saml2p:StatusCode'(Value: "urn:oasis:names:tc:SAML:2.0:status:Success")
                    }
                    mkp.yieldUnescaped ASSERTION_SIGNED
                }],
                [1, samlResponse(issuer() >> status() >> assertion())],
                [2, SAML_ONE_ASSERTION_SIGNED]
        ]
    }

    // todo: fix name of test
    @Unroll
    def "verify the test SAML Utility validator will reject a SAML response with an invalid signature #testNum"() {
        when: "we unmarshall the SAML string and try to validate the Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature)

        then: "the signature was not valid"
        !isValidSignature

        and: "the Issuer was set correctly"
        response.getIssuer().value == "http://idp.external.com"

        where:
        testNum | saml
        0       | SAML_ONE_ASSERTION_SIGNED.replace("    ", "")
        1       | SAML_ASSERTION_INVALID_SIGNATURE
    }
}
