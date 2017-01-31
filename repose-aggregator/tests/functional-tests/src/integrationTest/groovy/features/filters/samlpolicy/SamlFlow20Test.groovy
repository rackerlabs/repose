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
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response as DeproxyResponse
import spock.lang.Unroll

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static framework.mocks.MockIdentityV2Service.DEFAULT_MAPPING_POLICY
import static framework.mocks.MockIdentityV2Service.IDP_NO_RESULTS
import static framework.mocks.MockIdentityV2Service.createIdentityFaultJsonWithValues
import static framework.mocks.MockIdentityV2Service.createIdpJsonWithValues
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
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

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
    }

    def setup() {
        fakeIdentityV2Service.resetCounts()
        fakeIdentityV2Service.resetHandlers()
    }

    @Unroll
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

    def "a saml:response without an assertion should NOT be rejected with a 50x error"() {
        given: "a saml:response without an assertion"
        def saml = samlResponse(issuer() >> status())

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client does not get back a server-side error"
        (mc.receivedResponse.code as Integer) < SC_INTERNAL_SERVER_ERROR
        // TODO: Identity seems to need 2 assertions by their point, but it doesn't necessarily mean we should be
        // TODO: validating that we get at least 1 assertion here. Figure out what we should be validating, and clarify
        // TODO: this test if possible to a specific response code and response body.
    }

    def "a saml:response with an unsigned assertion should be rejected"() {
        given: "a saml:response with an unsigned assertion"
        def saml = samlResponse(issuer() >> status() >> assertion(fakeSign: false))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "All assertions must be signed"

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

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
    def "a saml:response with three assertions that are not all signed should be rejected - with signatures: #sigOne, #sigTwo, #sigThree"() {
        given: "a saml:response with three assertions that will each be signed depending on the test"
        def assertionOne = sigOne ? assertion(ASSERTION_SIGNED) : assertion(fakeSign: false)
        def assertionTwo = sigTwo ? assertion(ASSERTION_SIGNED_TWO) : assertion(fakeSign: false)
        def assertionThree = sigThree ? assertion(ASSERTION_SIGNED_THREE) : assertion(fakeSign: false)
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
        mc.receivedResponse.body as String == "SAML Response and all assertions need an issuer"

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

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

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        issuerOne | issuerTwo | issuerThree
        "same"    | "same"    | "diff"
        "same"    | "diff"    | "same"
        "diff"    | "same"    | "same"
        "one"     | "two"     | "three"
    }

    def "a saml:response with an Issuer that Identity doesn't know about should be rejected with a 401"() {
        given: "the IDP call will return an empty list"
        fakeIdentityV2Service.getIdpFromIssuerHandler = { String issuerParam, Request request ->
            new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): CONTENT_TYPE_JSON], IDP_NO_RESULTS)
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

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

    def "a saml:response with an Issuer that Identity doesn't have a mapping policy for should be rejected with a 401"() {
        given: "the mapping policy call will a 404"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_NOT_FOUND,
                    null,
                    [(CONTENT_TYPE): CONTENT_TYPE_JSON],
                    createIdentityFaultJsonWithValues(
                            name: "itemNotFound",
                            code: SC_NOT_FOUND,
                            message: "Identity Provider with id/name was not found."))
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

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

    def "when Identity returns an invalid mapping policy, Repose should return a 502"() {
        given: "the Identity mock will return an invalid mapping policy"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_OK,
                    null,
                    [(CONTENT_TYPE): CONTENT_TYPE_JSON],
                    '{"nope":{}}')
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_GATEWAY

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    def "when Identity returns a 500 from the mapping policy endpoint, Repose should return a 502"() {
        given: "the mapping policy call will not be successful"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_INTERNAL_SERVER_ERROR,
                    null,
                    [(CONTENT_TYPE): CONTENT_TYPE_JSON],
                    createIdentityFaultJsonWithValues(
                            name: "identityFault",
                            code: SC_INTERNAL_SERVER_ERROR,
                            message: "The default IDP policy is not properly configured."))
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_GATEWAY

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    def "Identity is queried with the correct Issuer when looking for the identity provider ID"() {
        given: "we're going to capture the requested issuer and path to Identity for the issuer to IDP ID call"
        String requestedIssuerParam = null
        String fullRequestPath = null
        fakeIdentityV2Service.getIdpFromIssuerHandler = { String issuerParam, Request request ->
            requestedIssuerParam = issuerParam
            fullRequestPath = request.path

            def body = createIdpJsonWithValues(issuer: issuerParam)
            def headers = [(CONTENT_TYPE): CONTENT_TYPE_JSON]
            new DeproxyResponse(SC_OK, null, headers, body)
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the requested issuer should match what was in the saml:response"
        requestedIssuerParam == samlIssuer

        and: "the raw path should not contain the issuer since it should have been URL encoded"
        !fullRequestPath.contains(samlIssuer)
    }

    def "Identity is queried with the correct IDP ID when looking for the identity provider mapping policy"() {
        given: "we're going to capture the requested IDP ID to Identity for the IDP ID to mapping policy call"
        String requestedIdpId = null
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            requestedIdpId = idpId
            new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): CONTENT_TYPE_JSON], DEFAULT_MAPPING_POLICY)
        }

        and: "we're going to ensure the IDP ID returned is unique"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the requested IDP ID should match what was returned from the first Identity call"
        requestedIdpId == idpId
    }
}
