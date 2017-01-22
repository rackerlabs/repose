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

import features.filters.samlpolicy.util.SamlUtilities
import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import groovy.xml.MarkupBuilder
import org.opensaml.saml.saml2.core.Response
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response as DeproxyResponse
import org.spockframework.runtime.ConditionNotSatisfiedError
import spock.lang.FailsWith
import spock.lang.Unroll

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static framework.mocks.MockIdentityV2Service.IDP_NO_RESULTS
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

/**
 * This functional test goes through the validation logic unique to Flow 2.0.
 */
class SamlFlow20Test extends ReposeValveTest {
    static samlUtilities = new SamlUtilities()
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
    }

    def setup() {
        fakeIdentityV2Service.resetCounts()
        fakeIdentityV2Service.resetHandlers()
        fakeIdentityV2Service.client_token = UUID.randomUUID().toString()
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response that is #signatureStatus will still be successfully processed as long as its Assertion is signed"() {
        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "2.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        //and: "the response from the origin service is appropriately updated by the filter"
        // TODO: what is Flow 2.0 going to do to the response?

        when: "the saml:response received by the origin service is unmarshalled"
        Response response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)

        then: "there should be two assertions"
        response.assertions.size() == 2

        and: "the first assertion and the saml:response should indicate that Repose is the Issuer"
        response.assertions[0].issuer.value == SAML_REPOSE_ISSUER
        response.issuer.value == SAML_REPOSE_ISSUER

        and: "the second Assertion should still have a valid signature and have the original Issuer"
        response.assertions[1].issuer.value == SAML_EXTERNAL_ISSUER
        samlUtilities.validateSignature(response.assertions[1].signature)

        and: "the saml:response should have a valid signature since Repose should have signed it"
        samlUtilities.validateSignature(response.signature)

        where:
        signatureStatus    | saml
        "not signed"       | SAML_ONE_ASSERTION_SIGNED
        "signed"           | SAML_ASSERTION_AND_MESSAGE_SIGNED
        "signed (invalid)" | SAML_ASSERTION_AND_MESSAGE_SIGNED.replaceFirst("\n", "").replaceFirst("\n", "")
    }

    def "a saml:response without an assertion should be rejected"() {
        given: "a saml:response without an assertion"
        def saml = samlResponse(issuer() >> status())

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an unsigned assertion should be rejected"() {
        given: "a saml:response with an unsigned assertion"
        def saml = samlResponse(issuer() >> status() >> assertion([:]))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "All assertions must be signed"
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_TEXT

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an assertion that has an invalid signature will still be processed successfully"() {
        given: "a saml:response with an assertion containing an invalid signature"
        def saml = samlResponse(issuer() >> status() >> assertion(ASSERTION_SIGNED.replace("    ", "")))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "2.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with three assertions that are not all signed should be rejected - with signatures: #sigOne, #sigTwo, #sigThree"() {
        given: "a saml:response with three assertions that will each be signed depending on the test"
        def assertionOne = sigOne ? assertion(ASSERTION_SIGNED) : assertion([:])
        def assertionTwo = sigTwo ? assertion(ASSERTION_SIGNED_TWO) : assertion([:])
        def assertionThree = sigThree ? assertion(ASSERTION_SIGNED_THREE) : assertion([:])
        def saml = samlResponse(issuer() >> status() >> assertionOne >> assertionTwo >> assertionThree)

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "All assertions must be signed"
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_TEXT

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        sigOne | sigTwo | sigThree
        false  | true   | true
        true   | false  | true
        true   | true   | false
        false  | false  | false
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with three signed assertions should be successful even if the signatures aren't valid - with valid signatures: #sigOne, #sigTwo, #sigThree"() {
        given: "a saml:response with three assertions that will each have a valid or invalid signature depending on the test"
        def assertionOne = sigOne ? ASSERTION_SIGNED : ASSERTION_SIGNED.replace("    ", "")
        def assertionTwo = sigTwo ? ASSERTION_SIGNED_TWO : ASSERTION_SIGNED_TWO.replace("    ", "")
        def assertionThree = sigThree ? ASSERTION_SIGNED_THREE : ASSERTION_SIGNED_THREE.replace("    ", "")
        def saml = samlResponse(issuer() >> status() >> assertion(assertionOne) >> assertion(assertionTwo) >> assertion(assertionThree))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "2.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        when: "the saml:response received by the origin service is unmarshalled"
        Response response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)

        then: "there should be four assertions"
        response.assertions.size() == 4

        and: "the first assertion and the saml:response should indicate that Repose is the Issuer"
        response.assertions[0].issuer.value == SAML_REPOSE_ISSUER
        response.issuer.value == SAML_REPOSE_ISSUER

        and: "the rest of the Assertions should have the original Issuer"
        response.assertions.drop(1).every { it.issuer.value == SAML_EXTERNAL_ISSUER }

        where:
        sigOne | sigTwo | sigThree
        true   | true   | true
        false  | true   | true
        true   | false  | true
        true   | true   | false
        false  | false  | false
    }

    def "a saml:response with an assertion missing the Issuer element should be rejected"() {
        given: "a saml:response with an assertion missing the Issuer element"
        def saml = samlResponse { MarkupBuilder builder ->
            builder.'saml2:Issuer'(SAML_EXTERNAL_ISSUER)
            builder.'saml2p:Status' {
                'saml2p:StatusCode'(Value: SAML_STATUS_SUCCESS)
            }
            builder.'saml2:Assertion'(ID: "_" + UUID.randomUUID().toString(), IssueInstant: "2013-11-15T16:19:06.310Z", Version: "2.0") {
                'saml2:Subject' {
                    'saml2:NameID'(Format: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", "john.doe")
                    'saml2:SubjectConfirmation'(Method: "urn:oasis:names:tc:SAML:2.0:cm:bearer") {
                        'saml2:SubjectConfirmationData'(NotOnOrAfter: "2113-11-17T16:19:06.298Z")
                    }
                }
                'saml2:AuthnStatement'(AuthnInstant: "2113-11-15T16:19:04.055Z") {
                    'saml2:AuthnContext' {
                        'saml2:AuthnContextClassRef'("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    }
                }
            }
            builder
        }

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an assertion containing an empty Issuer will still be processed successfully"() {
        given:
        def saml = samlResponse { MarkupBuilder builder ->
            builder.'saml2:Issuer'(SAML_EXTERNAL_ISSUER)
            builder.'saml2p:Status' {
                'saml2p:StatusCode'(Value: SAML_STATUS_SUCCESS)
            }
            builder.'saml2:Assertion'(ID: "_" + UUID.randomUUID().toString(), IssueInstant: "2013-11-15T16:19:06.310Z", Version: "2.0") {
                'saml2:Issuer'("")
                'saml2:Subject' {
                    'saml2:NameID'(Format: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", "john.doe")
                    'saml2:SubjectConfirmation'(Method: "urn:oasis:names:tc:SAML:2.0:cm:bearer") {
                        'saml2:SubjectConfirmationData'(NotOnOrAfter: "2113-11-17T16:19:06.298Z")
                    }
                }
                'saml2:AuthnStatement'(AuthnInstant: "2113-11-15T16:19:04.055Z") {
                    'saml2:AuthnContext' {
                        'saml2:AuthnContextClassRef'("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport")
                    }
                }
            }
            builder
        }

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "2.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with three assertions containing inconsistent Issuers should be rejected - with issuers: #issuerOne, #issuerTwo, #issuerThree"() {
        given:
        def saml = samlResponse(issuer() >> status() >>
                assertion(issuer: issuerOne, fakeSign: true) >>
                assertion(issuer: issuerTwo, fakeSign: true) >>
                assertion(issuer: issuerThree, fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "All assertions must come from the same issuer"
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_TEXT

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        issuerOne | issuerTwo | issuerThree
        "same"    | "same"    | "diff"
        "same"    | "diff"    | "same"
        "diff"    | "same"    | "same"
        "one"     | "two"     | "three"
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an Issuer that Identity doesn't know about (returns #description) should be rejected with a 401"() {
        given: "the IDP call will return the given response for this test"
        // TODO: verify this is what Identity would send for this case
        fakeIdentityV2Service.getIdpFromIssuerHandler = getIdpFromIssuerHandler

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def saml = samlResponse(issuer(generateUniqueIssuer()) >> status() >> assertion(fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        description     | getIdpFromIssuerHandler
        "a 404"         | { new DeproxyResponse(SC_NOT_FOUND) }
        "an empty list" | { new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): CONTENT_TYPE_JSON], IDP_NO_RESULTS) }
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an Issuer that Identity doesn't have a mapping policy for should be rejected with a 401"() {
        given: "the mapping policy call will return a 404"
        // TODO: verify this is what Identity would send for this case
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { new DeproxyResponse(SC_NOT_FOUND) }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def saml = samlResponse(issuer(generateUniqueIssuer()) >> status() >> assertion(fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }
}
