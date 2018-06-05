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

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.custommonkey.xmlunit.Diff
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.util.saml.SamlUtilities
import org.opensaml.saml.saml2.core.Response
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response as DeproxyResponse
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static javax.ws.rs.core.HttpHeaders.ACCEPT
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.*
import static org.openrepose.framework.test.mocks.MockIdentityV2Service.*
import static org.openrepose.framework.test.util.saml.SamlPayloads.*
import static org.openrepose.framework.test.util.saml.SamlUtilities.*

/**
 * This functional test goes through the validation logic unique to Flow 2.0.
 */
class SamlFlow20Test extends ReposeValveTest {
    static final String RESERVED_QUERY_PARAM_CHARS = "=+&#"
    static final String ENCODED_RESERVED_QUERY_PARAM_CHARS = "%3D%2B%26%23"

    static samlUtilities = new SamlUtilities()
    static jsonSlurper = new JsonSlurper()
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
    def "a saml:response with a(n) '#contentType' type body that is #signatureStatus will still be successfully processed as long as its Assertion is signed"() {
        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): contentType],
                requestBody: contentTypeBodyTransformers[contentType](saml))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == APPLICATION_XML
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
        samlUtilities.validateSignature(response.signature, getEntityIdForSignature(response.signature))

        where:
        contentType                 | signatureStatus    | saml
        APPLICATION_FORM_URLENCODED | "not signed"       | SAML_ONE_ASSERTION_SIGNED
        APPLICATION_FORM_URLENCODED | "signed"           | SAML_ASSERTION_AND_MESSAGE_SIGNED
        APPLICATION_FORM_URLENCODED | "signed (invalid)" | SAML_ASSERTION_AND_MESSAGE_SIGNED.replaceFirst("\n", "").replaceFirst("\n", "")
        APPLICATION_XML             | "not signed"       | SAML_ONE_ASSERTION_SIGNED
        APPLICATION_XML             | "signed"           | SAML_ASSERTION_AND_MESSAGE_SIGNED
        APPLICATION_XML             | "signed (invalid)" | SAML_ASSERTION_AND_MESSAGE_SIGNED.replaceFirst("\n", "").replaceFirst("\n", "")
    }

    @Unroll
    def "a saml:response with a(n) '#contentType' type body without an assertion should be rejected"() {
        given: "a saml:response without an assertion"
        def saml = samlResponse(issuer() >> status())

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): contentType],
                requestBody: contentTypeBodyTransformers[contentType](saml))

        then: "the client gets back a Bad Request response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST

        where:
        contentType << [APPLICATION_FORM_URLENCODED, APPLICATION_XML]
    }

    @Unroll
    def "a saml:response with a(n) '#contentType' type body and an unsigned assertion should be rejected"() {
        given: "a saml:response with an unsigned assertion"
        def saml = samlResponse(issuer() >> status() >> assertion(fakeSign: false))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): contentType],
                requestBody: contentTypeBodyTransformers[contentType](saml))

        then: "the client gets back a Bad Request response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "All assertions must be signed"

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        contentType << [APPLICATION_FORM_URLENCODED, APPLICATION_XML]
    }

    @Unroll
    def "a saml:response with a(n) '#contentType' type body and an assertion that has an invalid signature will still be processed successfully"() {
        given: "a saml:response with an assertion containing an invalid signature"
        def saml = samlResponse(issuer() >> status() >> assertion(ASSERTION_SIGNED.replace("    ", "")))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): contentType],
                requestBody: contentTypeBodyTransformers[contentType](saml))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == APPLICATION_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "2.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        where:
        contentType << [APPLICATION_FORM_URLENCODED, APPLICATION_XML]
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a Bad Request response"
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == APPLICATION_XML
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
                invalidSignature()(builder)
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a Bad Request response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
        mc.receivedResponse.body as String == "SAML Response and all assertions need an issuer"

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    def "a saml:response with an assertion containing an empty Issuer will be rejected"() {
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_BAD_REQUEST
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a Bad Request response"
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
            new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): APPLICATION_JSON], IDP_NO_RESULTS)
        }

        when:
        def mc = sendSamlRequestWithUniqueIssuer()

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()
    }

    @Unroll
    def "a saml:response with an Issuer that Identity doesn't have a #contentType mapping policy for should be rejected with a 401"() {
        given: "the mapping policy call will return a 404"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_NOT_FOUND,
                    null,
                    [(CONTENT_TYPE): contentType],
                    createIdentityFaultJsonWithValues(
                            name: "itemNotFound",
                            code: SC_NOT_FOUND,
                            message: "Identity Provider with id/name was not found."))
        }

        when:
        def mc = sendSamlRequestWithUniqueIssuer()

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        contentType << [APPLICATION_XML, APPLICATION_JSON, TEXT_YAML]
    }

    @Unroll
    def "when Identity returns an invalid #contentType mapping policy, Repose should return a 502"() {
        given: "the Identity mock will return an invalid mapping policy"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_OK,
                    null,
                    [(CONTENT_TYPE): contentType],
                    mappingPolicy)
        }

        when:
        def mc = sendSamlRequestWithUniqueIssuer()

        then: "the client gets back a bad response"
        mc.receivedResponse.code as Integer == SC_BAD_GATEWAY

        and: "the request doesn't get to the origin service"
        mc.handlings.isEmpty()

        where:
        [contentType, mappingPolicy] << [
            [APPLICATION_XML, createMappingXmlWithValues(rules: [[potato: [fries: [yummy: "yes"], hashBrowns: [:]]]])],
            [APPLICATION_JSON, createMappingJsonWithValues(rules: [[potato: [fries: [yummy: "yes"], hashBrowns: [:]]]])],
            [TEXT_YAML, createMappingYamlWithValues(rules: [[potato: [fries: [yummy: "yes"], hashBrowns: [:]]]])]
        ]
    }

    def "when Identity returns a malformed mapping policy, Repose should return a 502"() {
        given: "the Identity mock will return a malformed mapping policy (not valid JSON)"
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            new DeproxyResponse(
                    SC_OK,
                    null,
                    [(CONTENT_TYPE): APPLICATION_JSON],
                    "Hi, Principal Skinner! Hi, Super Nintendo Chalmers.")
        }

        when:
        def mc = sendSamlRequestWithUniqueIssuer()

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
                    [(CONTENT_TYPE): APPLICATION_JSON],
                    createIdentityFaultJsonWithValues(
                            name: "identityFault",
                            code: SC_INTERNAL_SERVER_ERROR,
                            message: "The default IDP policy is not properly configured."))
        }

        when:
        def mc = sendSamlRequestWithUniqueIssuer()

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
            def headers = [(CONTENT_TYPE): APPLICATION_JSON]
            new DeproxyResponse(SC_OK, null, headers, body)
        }

        and: "the Issuer is unique which will force the call to Identity (avoiding the cache)"
        def samlIssuerPart = generateUniqueIssuer()
        def samlIssuer = samlIssuerPart + RESERVED_QUERY_PARAM_CHARS
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the requested issuer should match what was in the saml:response"
        requestedIssuerParam == samlIssuer

        and: "the raw path should contain the query param encoded issuer"
        fullRequestPath.contains(samlIssuerPart + ENCODED_RESERVED_QUERY_PARAM_CHARS)
    }

    def "Identity is queried with the correct IDP ID when looking for the identity provider mapping policy"() {
        given: "we're going to capture the requested IDP ID to Identity for the IDP ID to mapping policy call"
        String requestedIdpId = null
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = { String idpId, Request request ->
            requestedIdpId = idpId
            new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): APPLICATION_JSON], DEFAULT_MAPPING_POLICY)
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
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the requested IDP ID should match what was returned from the first Identity call"
        requestedIdpId == idpId
    }

    def "an X-Auth-Token should not be sent to the origin service"() {
        when:
        def mc = sendSamlRequestWithUniqueIssuer()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service does not receive an X-Auth-Token header"
        mc.handlings[0].request.headers.getCountByName(X_AUTH_TOKEN) == 0
    }

    def "Repose does not alter the order of the assertions coming in the request"() {
        given: "a saml:response with a number of assertions (with unique data stored in the SPProvidedID)"
        def numOfAssertions = 7
        def samlIssuer = generateUniqueIssuer()
        def assertions = (1..numOfAssertions).inject({ MarkupBuilder builder -> builder }) { assertionsSoFar, num ->
            assertionsSoFar >> assertion(issuer: samlIssuer, spProvidedId: num as String, fakeSign: true)
        }
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertions)

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the saml:response received by the origin service is unmarshalled"
        Response response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)

        then: "there should be one extra assertion added by Repose"
        response.assertions.size() == numOfAssertions + 1

        and: "the original assertions are in the original order"
        response.assertions.drop(1).collect { it.subject.nameID.getSPProvidedID() } == (1..numOfAssertions)*.toString()
    }

    @Unroll
    def "a non-successful response code of #code from the origin service with content-type application/json will be returned to the client unaltered"() {
        given: "the origin service will return an Identity fault as JSON"
        fakeIdentityV2Service.generateTokenFromSamlResponseHandler = { Request request, boolean shouldReturnXml ->
            def values = [name: fault, code: code, message: message]
            new DeproxyResponse(code, null, [(CONTENT_TYPE): APPLICATION_JSON], createIdentityFaultJsonWithValues(values))
        }

        when: "the request is sent to Repose asking for a JSON response"
        def mc = sendSamlRequestWithUniqueIssuer((ACCEPT): APPLICATION_JSON)

        then: "the client receives the response code sent by the origin service"
        mc.receivedResponse.code as Integer == code

        when: "the fault response sent by the origin service is parsed as JSON"
        def sentResponseJson = jsonSlurper.parseText(mc.handlings[0].response.body as String)

        and: "the fault response received by the client is parsed as JSON"
        def receivedResponseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the JSON response was not altered by Repose"
        receivedResponseJson == sentResponseJson

        where:
        code                     | fault           | message
        SC_BAD_REQUEST           | "badRequest"    | "Error code: 'FED-006'; Subject is not specified"
        SC_INTERNAL_SERVER_ERROR | "identityFault" | "Service Unavailable"
    }

    @Unroll
    def "a non-successful response code of #code from the origin service with content-type application/xml will be returned to the client unaltered"() {
        given: "the origin service will return an Identity fault as XML"
        fakeIdentityV2Service.generateTokenFromSamlResponseHandler = { Request request, boolean shouldReturnXml ->
            def values = [name: fault, code: code, message: message]
            new DeproxyResponse(code, null, [(CONTENT_TYPE): APPLICATION_XML], createIdentityFaultXmlWithValues(values))
        }

        when: "the request is sent to Repose asking for an XML response"
        def mc = sendSamlRequestWithUniqueIssuer((ACCEPT): APPLICATION_XML)

        then: "the client receives the response code sent by the origin service"
        mc.receivedResponse.code as Integer == code

        and: "the XML sent by the origin service and received by the client are similar enough"
        new Diff(mc.handlings[0].response.body as String, mc.receivedResponse.body as String).similar()

        where:
        code                     | fault           | message
        SC_BAD_REQUEST           | "badRequest"    | "Saml response issueInstant cannot be in the future."
        SC_INTERNAL_SERVER_ERROR | "identityFault" | "Internal Server Error"
    }

    @Unroll
    def "a response from the origin service with content-type #contentType that is either unsupported or the content is malformed will result in a Bad Gateway (502) response"() {
        given: "the origin service will return a response that can't be parsed as the specified content-type"
        def responseBody = "This is neither JSON nor XML, but is plain text."
        fakeIdentityV2Service.generateTokenFromSamlResponseHandler = { Request request, boolean shouldReturnXml ->
            new DeproxyResponse(SC_OK, null, [(CONTENT_TYPE): contentType], responseBody)
        }

        when: "the request is sent to Repose asking for a response with the specified content-type"
        def mc = sendSamlRequestWithUniqueIssuer((ACCEPT): contentType)

        then: "the origin service returns the malformed data"
        mc.handlings[0]
        mc.handlings[0].response.body as String == responseBody

        and: "the client receives a 502"
        mc.receivedResponse.code as Integer == SC_BAD_GATEWAY

        where:
        contentType << [APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN]
    }

    def sendSamlRequestWithUniqueIssuer(Map headers = [:]) {
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED] + headers,
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
    }
}
