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
import org.opensaml.saml.saml2.core.Response
import org.rackspace.deproxy.Deproxy
import org.spockframework.runtime.ConditionNotSatisfiedError
import spock.lang.FailsWith
import spock.lang.Unroll

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This functional test goes through the validation logic unique to Flow 1.0.
 */
class SamlFlow10Test extends ReposeValveTest {
    static samlUtilities = new SamlUtilities()
    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/flow1_0", params)

        deproxy = new Deproxy()

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)
    }

    def setup() {
        fakeIdentityV2Service.resetCounts()
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with an Issuer that #isIt in the configured policy-bypass-issuers list will have an 'Identity-API-Version' value of #headerValue"() {
        given:
        def body = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(
                samlResponse(issuer(samlIssuer) >> status() >> assertion())))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: body)

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header value"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == headerValue
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        and: "Identity is queried for the policy mapping the correct number of times for the given flow"
        fakeIdentityV2Service.getIdpFromIssuerCount + fakeIdentityV2Service.getMappingPolicyForIdpCount == policyMappingQueries

        where:
        isIt     | headerValue | samlIssuer                       | policyMappingQueries
        "is"     | "1.0"       | "http://legacy.idp.external.com" | 0
        "is not" | "2.0"       | generateUniqueIssuer()           | 2
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response with a Flow 1.0 Issuer will work despite having #flow20ValidationIssue"() {
        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service received the request with the correct header value"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "1.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        and: "Identity is not queried for a policy mapping"
        fakeIdentityV2Service.getIdpFromIssuerCount == 0
        fakeIdentityV2Service.getMappingPolicyForIdpCount == 0

        //and: "the response from the origin service is not updated by the filter"
        // TODO: what is Flow 2.0 going to do to the response?

        where:
        saml                                     | flow20ValidationIssue
        SAML_CRAZY_INVALID                       | "assertions with inconsistent Issuers, missing Issuers, missing signatures, and an invalid signature"
        samlResponse(issuer(SAML_LEGACY_ISSUER)) | "no other fields in it"
    }

    @Unroll
    @FailsWith(ConditionNotSatisfiedError)
    def "a saml:response that #signedState will not be altered and will maintain signature validity through Repose"() {
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
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "1.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        and: "Identity is not queried for a policy mapping"
        fakeIdentityV2Service.getIdpFromIssuerCount == 0
        fakeIdentityV2Service.getMappingPolicyForIdpCount == 0

        //and: "the response from the origin service is not updated by the filter"
        // TODO: what is Flow 2.0 going to do to the response?

        when: "the saml:response received by the origin service is unmarshalled"
        Response response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)

        then: "any signatures in the XML are still valid upon arriving at the origin service"
        !validateResponse || samlUtilities.validateSignature(response.signature)
        !validateAssertion || samlUtilities.validateSignature(response.assertions[0].signature)

        where:
        saml                                            | signedState                            | validateResponse | validateAssertion
        SAML_LEGACY_ISSUER_UNSIGNED                     | "is not signed"                        | false            | false
        SAML_LEGACY_ISSUER_SIGNED_ASSERTION             | "has a signed assertion"               | false            | true
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE               | "is signed"                            | true             | false
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE_AND_ASSERTION | "has a signed assertion and is signed" | true             | true
    }
}
