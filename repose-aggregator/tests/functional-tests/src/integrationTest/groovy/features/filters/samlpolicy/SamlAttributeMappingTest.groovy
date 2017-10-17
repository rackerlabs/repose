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
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.util.saml.SamlUtilities
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.Response as SamlResponse
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.ACCEPT
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.*
import static org.openrepose.framework.test.mocks.MockIdentityV2Service.*
import static org.openrepose.framework.test.util.saml.SamlPayloads.*
import static org.openrepose.framework.test.util.saml.SamlUtilities.*

/**
 * This functional test ensures the attribute mappings are being set correctly on the request and the response.
 */
class SamlAttributeMappingTest extends ReposeValveTest {
    static samlUtilities = new SamlUtilities()
    static xmlSlurper = new XmlSlurper()
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
        fakeIdentityV2Service.resetHandlers()
    }

    @Unroll
    def "the saml:response will be translated before being sent to the origin service #withOut path function in #acceptType policy"() {
        given: "a mapping policy with a literal value and a path-based value in addition to the standard attributes"
        def extAttribLiteral = "banana"
        def extAttribLiteralValue = "phone"
        def extAttribPath = "adventure"
        def extAttribPathValue = "Mordor"
        def mappingPolicy = createMappingYamlWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): extAttribPathPolicy],
                remote: remoteValue)

        and: "a saml:response with a value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def attribName = "Frodo Baggins"
        def attribRole = "nova:cool_cat"
        def attribDomain = "193083"
        def attribEmail = "frodo.baggins@nowhere.abc"
        def attribGroup = "Hobbits"
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
                issuer: samlIssuer,
                name: attribName,
                attributes: [roles: [attribRole], domain: [attribDomain], email: [attribEmail], group: [attribGroup]],
                spProvidedId: extAttribPathValue,
                fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
                .createGetMappingPolicyForIdp(acceptType, [mappings: [(idpId): mappingPolicy]])

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request"
        mc.handlings[0]

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the default attributes are set correctly"
        attributes.find { it.name == "roles" }.attributeValues[0].value == attribRole
        attributes.find { it.name == "domain" }.attributeValues[0].value == attribDomain
        attributes.find { it.name == "email" }.attributeValues[0].value == attribEmail
        attributes.find { it.name == "group" }.attributeValues[0].value == attribGroup

        and: "the extended attributes are set correctly"
        attributes.find {
            it.name == "user/$extAttribLiteral" as String
        }.attributeValues[0].value == extAttribLiteralValue
        attributes.find { it.name == "user/$extAttribPath" as String }.attributeValues[0].value == extAttribPathValue

        where:
        [withOut, extAttribPathPolicy, remoteValue, acceptType] << [
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], APPLICATION_JSON],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, APPLICATION_JSON],
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], TEXT_YAML],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, TEXT_YAML]
        ]
    }

    @Unroll
    def "the access response (JSON) from the origin service will be translated before being sent to the client #withOut path function in #acceptType policy"() {
        given: "a mapping policy with a literal value and a path-based value in addition to the standard attributes"
        def extAttribLiteral = "potato"
        def extAttribLiteralValue = "salad"
        def extAttribPath = "pie"
        def extAttribPathValue = "eye"
        def mappingPolicy = createMappingYamlWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): extAttribPathPolicy],
                remote: remoteValue)

        and: "a saml:response with a value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
                issuer: samlIssuer,
                spProvidedId: extAttribPathValue,
                fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
            .createGetMappingPolicyForIdp(acceptType, [mappings: [(idpId): mappingPolicy]])

        when: "a request is sent to Repose requesting a JSON response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client receives JSON successfully"
        mc.receivedResponse.code as Integer == SC_OK
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == APPLICATION_JSON

        when: "the response sent to the client is parsed as JSON"
        def json = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the extended attributes are set correctly"
        json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribLiteral" == extAttribLiteralValue
        json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribPath" == extAttribPathValue

        where:
        [withOut, extAttribPathPolicy, remoteValue, acceptType] << [
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], APPLICATION_JSON],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, APPLICATION_JSON],
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], TEXT_YAML],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, TEXT_YAML]
        ]
    }

    @Unroll
    def "the access response (XML) from the origin service will be translated before being sent to the client #withOut path function in #acceptType policy"() {
        given: "a mapping policy with a fixed value and a dynamic value in addition to the standard attributes"
        def extAttribLiteral = "blues"
        def extAttribLiteralValue = "clues"
        def extAttribPath = "Swiper"
        def extAttribPathValue = "no swiping plz"
        def mappingPolicy = createMappingYamlWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): extAttribPathPolicy],
                remote: remoteValue)

        and: "a saml:response with a value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
                issuer: samlIssuer,
                spProvidedId: extAttribPathValue,
                fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
            .createGetMappingPolicyForIdp(acceptType, [mappings: [(idpId): mappingPolicy]])

        when: "a request is sent to Repose requesting an XML response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_XML],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client receives XML successfully"
        mc.receivedResponse.code as Integer == SC_OK
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == APPLICATION_XML

        when: "the response sent to the client is parsed as XML"
        def access = xmlSlurper.parseText(mc.receivedResponse.body as String)

        and: "we get the 'user' group in the extended attributes"
        def userGroup = access.extendedAttributes.'*'.find { it.@name == "user" }

        then: "the extended attributes are set correctly"
        userGroup.'*'.find { it.@name == extAttribLiteral }.value[0].text() == extAttribLiteralValue
        userGroup.'*'.find { it.@name == extAttribPath }.value[0].text() == extAttribPathValue

        where:
        [withOut, extAttribPathPolicy, remoteValue, acceptType] << [
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], APPLICATION_JSON],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, APPLICATION_JSON],
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], TEXT_YAML],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, TEXT_YAML]
        ]
    }

    @Unroll
    def "the correct translation will be used on the request and response when cached #withOut path function in #acceptType policy"() {
        given: "mapping policies with a literal value and a path-based value for three issuers"
        def numOfIssuers = 3
        def extAttribLiteral = "color"
        def extAttribLiteralValues = ["red", "green", "blue"]
        def extAttribPath = "shape"
        def extAttribPathValues = ["square", "circle", "triangle"]
        def mappingPolicies = extAttribLiteralValues.collect { extAttribLiteralValue ->
            createMappingYamlWithValues(
                    userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): extAttribPathPolicy],
                    remote: remoteValue)
        }

        and: "saml:responses with a value at the path specified by the mapping policies"
        def samlIssuers = (1..numOfIssuers).collect { generateUniqueIssuer() }
        def samls = [samlIssuers, extAttribPathValues].transpose().collect { samlIssuer, extAttribPathValue ->
            samlResponse(issuer(samlIssuer) >> status() >> assertion(
                    issuer: samlIssuer,
                    spProvidedId: extAttribPathValue,
                    fakeSign: true))
        }

        and: "a different set of path values in the saml:responses will be sent for the second round of requests"
        def extAttribPathValuesRoundTwo = ["polygon", "tesseract", "hexadecachoron"]
        def samlsRoundTwo = [samlIssuers, extAttribPathValuesRoundTwo].transpose().collect { samlIssuer, extAttribPathValue ->
            samlResponse(issuer(samlIssuer) >> status() >> assertion(
                    issuer: samlIssuer,
                    spProvidedId: extAttribPathValue,
                    fakeSign: true))
        }

        and: "an Identity mock that will return the correct mapping policy for each issuer/IDP ID"
        def idpIds = (1..numOfIssuers).collect { generateUniqueIdpId() }
        Map issuerToIdpId = [samlIssuers, idpIds].transpose().collectEntries()
        fakeIdentityV2Service.getIdpFromIssuerHandler = { String issuer, Request request ->
            new Response(
                    SC_OK,
                    null,
                    [(CONTENT_TYPE): APPLICATION_JSON],
                    createIdpJsonWithValues(issuer: issuer, id: issuerToIdpId.get(issuer)))
        }
        Map idpIdToMapping = [idpIds, mappingPolicies].transpose().collectEntries()
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
                .createGetMappingPolicyForIdp(acceptType, [mappings: idpIdToMapping])

        when: "requests are sent to Repose for the first time for each issuer"
        def mcs = samls.collect { saml ->
            deproxy.makeRequest(
                    url: reposeEndpoint + SAML_AUTH_URL,
                    method: HTTP_POST,
                    headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_JSON],
                    requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
        }

        then: "the origin service receives all of the requests and the client receives all of the responses"
        mcs.every { it.handlings[0] }
        mcs.every { it.receivedResponse.code as Integer == SC_OK }

        when: "the saml:responses received by the origin service are unmarshalled"
        List<SamlResponse> responses = mcs.collect {
            samlUtilities.unmarshallResponse(it.handlings[0].request.body as String)
        }
        List<List<Attribute>> attributesPerResponse = responses.collect {
            it.assertions[0].attributeStatements[0].attributes
        }

        then: "all of the requests have two assertions"
        responses.every { it.assertions.size() == 2 }

        and: "the extended attributes are set correctly in the requests"
        attributesPerResponse.collect { attributes ->
            attributes.find { it.name == "user/$extAttribLiteral" as String }.attributeValues[0].value
        } == extAttribLiteralValues

        attributesPerResponse.collect { attributes ->
            attributes.find { it.name == "user/$extAttribPath" as String }.attributeValues[0].value
        } == extAttribPathValues

        when: "the responses sent to the client are parsed as JSON"
        def jsons = mcs.collect { jsonSlurper.parseText(it.receivedResponse.body as String) }

        then: "the extended attributes are set correctly in the responses"
        jsons.collect { it.access.'RAX-AUTH:extendedAttributes'.user."$extAttribLiteral" } == extAttribLiteralValues
        jsons.collect { it.access.'RAX-AUTH:extendedAttributes'.user."$extAttribPath" } == extAttribPathValues

        when: "we disable the Identity mock handlers"
        fakeIdentityV2Service.getIdpFromIssuerHandler = null
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = null

        and: "the requests for round two are sent with the different values at the path-referenced attribute"
        mcs = samlsRoundTwo.collect { saml ->
            deproxy.makeRequest(
                    url: reposeEndpoint + SAML_AUTH_URL,
                    method: HTTP_POST,
                    headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_JSON],
                    requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
        }

        then: "the origin service receives all of the requests and the client receives all of the responses"
        mcs.every { it.handlings[0] }
        mcs.every { it.receivedResponse.code as Integer == SC_OK }

        when: "the saml:responses received by the origin service are unmarshalled"
        responses = mcs.collect { samlUtilities.unmarshallResponse(it.handlings[0].request.body as String) }
        attributesPerResponse = responses.collect { it.assertions[0].attributeStatements[0].attributes }

        then: "all of the requests have two assertions"
        responses.every { it.assertions.size() == 2 }

        and: "the extended attributes are set correctly in the requests"
        attributesPerResponse.collect { attributes ->
            attributes.find { it.name == "user/$extAttribLiteral" as String }.attributeValues[0].value
        } == extAttribLiteralValues

        attributesPerResponse.collect { attributes ->
            attributes.find { it.name == "user/$extAttribPath" as String }.attributeValues[0].value
        } == extAttribPathValuesRoundTwo

        when: "the responses sent to the client are parsed as JSON"
        jsons = mcs.collect { jsonSlurper.parseText(it.receivedResponse.body as String) }

        then: "the extended attributes are set correctly in the responses"
        jsons.collect { it.access.'RAX-AUTH:extendedAttributes'.user."$extAttribLiteral" } == extAttribLiteralValues
        jsons.collect { it.access.'RAX-AUTH:extendedAttributes'.user."$extAttribPath" } == extAttribPathValuesRoundTwo

        where:
        [withOut, extAttribPathPolicy, remoteValue, acceptType] << [
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], APPLICATION_JSON],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, APPLICATION_JSON],
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], TEXT_YAML],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, TEXT_YAML]
        ]
    }

    def "the extended attributes section is not added to the request/response when the mapping policy does not include any"() {
        given: "a mapping policy with only the defaults set and a saml:response with a unique issuer"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when:
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the request does not contain any extended attributes"
        !attributes.any { it.name.contains("/") }

        when: "the response sent to the client is parsed as JSON"
        def json = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response does not contain any extended attributes"
        !json.access.'RAX-AUTH:extendedAttributes'
    }

    @Unroll
    def "when the specified path for an extended attribute is not present in the saml:response, it is not added to the request/response #withOut path function in #acceptType policy"() {
        given: "a mapping policy with a literal value and a path-based value (that won't be in the saml:response)"
        def extAttribLiteral = "Captain"
        def extAttribLiteralValue = "Planet"
        def extAttribPath = "cake"
        def mappingPolicy = createMappingYamlWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): extAttribPathPolicy],
                remote: remoteValue)

        and: "a saml:response with no value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
            .createGetMappingPolicyForIdp(acceptType, [mappings: [(idpId): mappingPolicy]])

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED, (ACCEPT): APPLICATION_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the extended attribute for the literal value is set in the request"
        attributes.find {
            it.name == "user/$extAttribLiteral" as String
        }.attributeValues[0].value == extAttribLiteralValue

        // TODO: This may be a bug in the attibuteMapping library.
        // TODO: It is adding an extended attribute that references a path in the SAML Response even though it doesn't exist.
        and: "the extended attribute for the path value is not in the request"
        !attributes.any { it.name == "user/$extAttribPath" as String }

        when: "the response sent to the client is parsed as JSON"
        def json = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the extended attribute for the literal value is set in the response"
        json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribLiteral" == extAttribLiteralValue

        and: "the extended attribute for the path value is not in the response"
        !json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribPath"

        where:
        [withOut, extAttribPathPolicy, remoteValue, acceptType] << [
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], APPLICATION_JSON],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, APPLICATION_JSON],
            ["without", "{0}", [[path: $//saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID/$]], TEXT_YAML],
            ["with", "{Pt(/saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID)}", null, TEXT_YAML]
        ]
    }

    @Unroll
    def "the saml:response will be translated before being sent to the origin service #testName"() {
        given: "a mapping policy with a literal value and a path-based value in addition to the standard attributes"
        def extAttribLiteral = "Rick"
        def extAttribLiteralValue = "Wubbalubbadubdub"
        def extAttribPath = "Morty"
        def extAttribPathValue = "IDontKnowAboutThis"
        def mappingPolicy =
                """${prefix}mapping:
                |  rules:
                |  - local:
                |      user:
                |        domain: '{D}'
                |        name: '{D}'
                |        email: '{D}'
                |        group: '{D}'
                |        roles: '{D}'
                |        expire: '{D}'
                |        $extAttribLiteral: $extAttribLiteralValue
                |        $extAttribPath: '{0}'
                |    remote:
                |    - path: /saml2p:Response/saml2:Assertion/saml2:Subject/saml2:NameID/@SPProvidedID
                |  version: RAX-1$suffix""".stripMargin()

        and: "a saml:response with a value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def attribName = "Bird Person"
        def attribRole = "Tammy:GubbaNubNubDoRaKa"
        def attribDomain = "193083"
        def attribEmail = "BirdNTammy@GetSchwifty.com"
        def attribGroup = "Avianpeople"
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
                issuer: samlIssuer,
                name: attribName,
                attributes: [roles: [attribRole], domain: [attribDomain], email: [attribEmail], group: [attribGroup]],
                spProvidedId: extAttribPathValue,
                fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
                .createGetMappingPolicyForIdp(TEXT_YAML, [mappings: [(idpId): mappingPolicy]])

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service receives the request"
        mc.handlings[0]

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the default attributes are set correctly"
        attributes.find { it.name == "roles" }.attributeValues[0].value == attribRole
        attributes.find { it.name == "domain" }.attributeValues[0].value == attribDomain
        attributes.find { it.name == "email" }.attributeValues[0].value == attribEmail
        attributes.find { it.name == "group" }.attributeValues[0].value == attribGroup

        and: "the extended attributes are set correctly"
        attributes.find {
            it.name == "user/$extAttribLiteral" as String
        }.attributeValues[0].value == extAttribLiteralValue
        attributes.find { it.name == "user/$extAttribPath" as String }.attributeValues[0].value == extAttribPathValue

        where:
        [testName, prefix, suffix] << [
                ["without explicit start", "", ""],
                ["with explicit start", "---\n", ""],
                ["with extra pre-whitespace and explicit start", "\n\n---\n", ""],
                ["with extra pre-whitespace", "\n\n", ""],
                ["with extra post-whitespace", "", "\n\n"],
        ]
    }

    @Unroll
    def "the translated saml:response will make it to the origin service when the #policyFormat policy uses the default domain SAML attribute"() {
        given: "a saml:response with a domain"
        def samlIssuer = generateUniqueIssuer()
        def attribDomain = "193083"
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
            issuer: samlIssuer,
            attributes: [domain: [attribDomain]],
            fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String approvedDomainId = "123456"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(
            id: idpId,
            approvedDomainIds: [approvedDomainId])
        fakeIdentityV2Service.createGetMappingPolicyForIdp(policyFormat)

        when: "a request is sent to Repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + SAML_AUTH_URL,
            method: HTTP_POST,
            headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
            requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service will receive the request"
        mc.handlings.size() == 1

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the domain attribute is set to the SAML attribute domain"
        attributes.find { it.name == "domain" }.attributeValues[0].value == attribDomain

        where:
        policyFormat << [TEXT_YAML, APPLICATION_JSON]
    }

    @Unroll
    def "the translated saml:response will make it to the origin service when the #policyFormat policy uses the default domain and the IDP provides one approved domain"() {
        given: "a saml:response with a domain"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
            issuer: samlIssuer,
            attributes: [domain: []],
            fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String approvedDomainId = "123456"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(
            id: idpId,
            approvedDomainIds: [approvedDomainId])
        fakeIdentityV2Service.createGetMappingPolicyForIdp(policyFormat)

        when: "a request is sent to Repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + SAML_AUTH_URL,
            method: HTTP_POST,
            headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
            requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service will receive the request"
        mc.handlings.size() == 1

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the domain attribute is set to the IDP approved domain"
        attributes.find { it.name == "domain" }.attributeValues[0].value == approvedDomainId

        where:
        policyFormat << [TEXT_YAML, APPLICATION_JSON]
    }

    @Unroll
    def "the translated saml:response will make it to the origin service when the #policyFormat policy uses the default domain and the IDP provides #approvedDomainsMultiplicity approved domains"() {
        given: "a saml:response without a domain attribute"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
            issuer: samlIssuer,
            attributes: [domain: []],
            fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(
            id: idpId,
            approvedDomainIds: approvedDomainIds)
        fakeIdentityV2Service.createGetMappingPolicyForIdp(policyFormat)

        when: "a request is sent to Repose"
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + SAML_AUTH_URL,
            method: HTTP_POST,
            headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
            requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the origin service will receive the request"
        mc.handlings.size() == 1

        when: "the saml:response received by the origin service is unmarshalled"
        SamlResponse response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        List<Attribute> attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the domain attribute is not set"
        attributes.find { it.name == "domain" }.attributeValues[0].value == null

        where:
        [approvedDomainsMultiplicity, approvedDomainIds, policyFormat] << [
            ["no", [], TEXT_YAML],
            ["multiple", ["098765", "987654"], TEXT_YAML],
            ["no", [], APPLICATION_JSON],
            ["multiple", ["098765", "987654"], APPLICATION_JSON],
        ]
    }
}
