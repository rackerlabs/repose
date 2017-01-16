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
import org.openrepose.commons.utils.http.CommonHttpHeader
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
    static final String AUTH_TOKEN = CommonHttpHeader.AUTH_TOKEN.toString()

    static xmlSlurper = new XmlSlurper()
    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(params.identityPort, params.targetPort)
        deproxy.addEndpoint(params.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)
    }

    @Unroll
    def "a valid request will make it to the origin service and back to the client successfully with form parameters: #formParams.keySet()"() {
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: headers,
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
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): contentType,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: headers,
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
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: headers,
                requestBody: asUrlEncodedForm((PARAM_EXTRANEOUS): SAML_ONE_ASSERTION_SIGNED_BASE64))

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    def "a request without any parameters nor a request body should be rejected"() {
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: headers)

        then: "the request is rejected"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the origin service does not receive the request"
        mc.handlings.isEmpty()
    }

    @Unroll
    def "a request using the HTTP method #httpMethod should be rejected"() {
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: httpMethod,
                headers: headers,
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
        given:
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED,
                       (AUTH_TOKEN): fakeIdentityV2Service.client_token]

        when: "we make a POST request"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: HTTP_POST,
                headers: headers,
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
}
