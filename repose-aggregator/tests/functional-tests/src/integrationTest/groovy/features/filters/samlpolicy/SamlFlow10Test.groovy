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
import groovy.json.JsonSlurper
import org.custommonkey.xmlunit.Diff
import org.opensaml.saml.saml2.core.Response
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This functional test goes through the validation logic unique to Flow 1.0.
 */
class SamlFlow10Test extends ReposeValveTest {
    static samlUtilities = new SamlUtilities()
    static xmlSlurper = new XmlSlurper()
    static jsonSlurper = new JsonSlurper()
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

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
    }

    def setup() {
        fakeIdentityV2Service.with {
            resetCounts()
            getIdpFromIssuerHandler = null
            getMappingPolicyForIdpHandler = null
        }
    }

    @Unroll
    def "a saml:response with an Issuer that #isIt in the configured policy-bypass-issuers list will get an 'Identity-API-Version' value of #headerValue in the request to the origin service"() {
        given:
        def body = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(
                samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))))

        and: "the Identity mocks are available for the Flow 2.0 call"
        fakeIdentityV2Service.resetHandlers()

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: body)

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service receives the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getCountByName(CONTENT_TYPE) == 1
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getCountByName(IDENTITY_API_VERSION) == 1
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
    def "a saml:response with a Flow 1.0 Issuer will still be successfully processed despite having #flow20ValidationIssue"() {
        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service receives the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "1.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        and: "Identity is not queried for a policy mapping"
        fakeIdentityV2Service.getIdpFromIssuerCount == 0
        fakeIdentityV2Service.getMappingPolicyForIdpCount == 0

        where:
        saml                                     | flow20ValidationIssue
        SAML_CRAZY_INVALID                       | "assertions with inconsistent Issuers, missing Issuers, missing signatures, and an invalid signature"
        samlResponse(issuer(SAML_LEGACY_ISSUER)) | "no other fields in it other than the Issuer element"
    }

    @Unroll
    def "a saml:response that #signedState will not be altered and will maintain signature validity through Repose"() {
        when: "a request containing a saml:response with a legacy Issuer is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client gets back a good response"
        mc.receivedResponse.code as Integer == SC_OK

        and: "the origin service receives the request with the correct header values"
        mc.handlings[0]
        mc.handlings[0].request.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML
        mc.handlings[0].request.headers.getFirstValue(IDENTITY_API_VERSION) == "1.0"
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == 1

        and: "Identity is not queried for a policy mapping"
        fakeIdentityV2Service.getIdpFromIssuerCount == 0
        fakeIdentityV2Service.getMappingPolicyForIdpCount == 0

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

    def "the response to the client should not be translated by Repose when the saml:response has a Flow 1.0 Issuer"() {
        given: "a saml:response with a legacy issuer"
        def saml = samlResponse(issuer(SAML_LEGACY_ISSUER) >> status() >> assertion(issuer: SAML_LEGACY_ISSUER))

        when: "the request is sent to Repose asking for a JSON response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED, (ACCEPT): CONTENT_TYPE_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the 'access' response sent by the origin service is parsed as JSON"
        def sentResponseJson = jsonSlurper.parseText(mc.handlings[0].response.body as String)

        and: "the 'access' response received by the client is parsed as JSON"
        def receivedResponseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "there should not be any extended attributes in the JSON received by the client"
        !receivedResponseJson.access.'RAX-AUTH:extendedAttributes'

        and: "the JSON response was not altered by Repose"
        receivedResponseJson == sentResponseJson
    }

    def "the JSON response to the client should not be translated by Repose even if the saml:response contained extended attributes when it has a Flow 1.0 Issuer"() {
        given: "a saml:response with a legacy issuer and the extended attribute 'user/foo'"
        def saml = samlResponse(issuer(SAML_LEGACY_ISSUER) >> status() >> assertion(
                issuer: SAML_LEGACY_ISSUER,
                attributes: [roles: ["nova:admin"], domain: ["827319"], email: ["popeye@sailor.man"], "user/foo": ["bar"]]))

        when: "the request is sent to Repose asking for a JSON response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED, (ACCEPT): CONTENT_TYPE_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the 'access' response sent by the origin service is parsed as JSON"
        def sentResponseJson = jsonSlurper.parseText(mc.handlings[0].response.body as String)

        and: "the 'access' response received by the client is parsed as JSON"
        def receivedResponseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "there should not be any extended attributes in the JSON received by the client"
        !receivedResponseJson.access.'RAX-AUTH:extendedAttributes'

        and: "the JSON response was not altered by Repose"
        receivedResponseJson == sentResponseJson
    }

    def "the XML response to the client should not be translated by Repose even if the saml:response contained extended attributes when it has a Flow 1.0 Issuer"() {
        given: "a saml:response with a legacy issuer and the extended attribute 'user/foo'"
        def saml = samlResponse(issuer(SAML_LEGACY_ISSUER) >> status() >> assertion(
                issuer: SAML_LEGACY_ISSUER,
                attributes: [roles: ["nova:admin"], domain: ["827319"], email: ["popeye@sailor.man"], "user/foo": ["baz"]]))

        when: "the request is sent to Repose asking for an XML response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED, (ACCEPT): CONTENT_TYPE_XML],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the response received by the client is parsed as XML"
        def access = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response does not contain any extended attributes"
        !access.'RAX-AUTH:extendedAttributes'

        and: "the XML sent by the origin service and received by the client are similar enough"
        new Diff(mc.handlings[0].response.body as String, mc.receivedResponse.body as String).similar()
    }
}
