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
import framework.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static javax.servlet.http.HttpServletResponse.*

/**
 * This functional test goes through the shared validation logic between Flow 1.0 and 2.0 including the requirements
 * for the HTTP Method, Content-Type, and base64 encoding.
 */
class SamlBasicValidationTest extends ReposeValveTest {
    static xmlSlurper = new XmlSlurper()
    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)

        deproxy = new Deproxy()

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
    }

    @Unroll
    def "a valid request will make it to the origin service and back to the client successfully with form parameters: #formParams.keySet()"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm(formParams))

        then: "the request is successfully processed"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request as valid XML"
        mc.handlings[0]
        mc.handlings[0].request.headers.getCountByName(CONTENT_TYPE) == 1
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML

        and: "the request body received by the origin service can be parsed as XML"
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
                url: reposeEndpoint + SAML_AUTH_URL,
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
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_EXTRANEOUS): SAML_ONE_ASSERTION_SIGNED_BASE64))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "No SAMLResponse value found"

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    def "a request without any parameters nor a request body should be rejected"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED])

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "No SAMLResponse value found"

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    @Unroll
    def "a request using the HTTP method #httpMethod should be rejected"() {
        when: "we make a request with the given HTTP method"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
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
    def "a request should be rejected when the SAMLResponse contents are invalid due to #reason"() {
        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): paramValue))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == expectedResponse

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()

        where:
        reason                    | paramValue                                                        | expectedResponse
        "invalid base64 encoding" | SAML_ONE_ASSERTION_SIGNED_BASE64 + "!@#%^*)*)@"                   | "SAMLResponse is not in valid Base64 scheme"
        "invalid XML"             | encodeBase64("legit saml response kthxbai")                       | "" // TODO: figure out what the error is going to be
        "invalid SAML"            | encodeBase64("<banana/>")                                         | "" // TODO: figure out what the error is going to be
        "missing Issuer element"  | encodeBase64(samlResponse(status() >> assertion()))               | "No issuer present in SAML Response"
        "empty Issuer element"    | encodeBase64(samlResponse(issuer("") >> status() >> assertion())) | "No issuer present in SAML Response"
    }
}
