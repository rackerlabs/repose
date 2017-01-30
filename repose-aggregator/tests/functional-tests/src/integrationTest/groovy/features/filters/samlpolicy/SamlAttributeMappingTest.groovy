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
import org.rackspace.deproxy.Deproxy

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static framework.mocks.MockIdentityV2Service.createMappingJsonWithValues
import static javax.servlet.http.HttpServletResponse.SC_OK

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
        fakeIdentityV2Service.resetCounts()
        fakeIdentityV2Service.resetHandlers()
    }

    def "the saml:response will be translated before being sent to the origin service"() {
        given: "a mapping policy with a literal value and a path-based value in addition to the standard attributes"
        def extAttribLiteral = "banana"
        def extAttribLiteralValue = "phone"
        def extAttribPath = "adventure"
        def extAttribPathValue = "Mordor"
        def mappingPolicy = createMappingJsonWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): "{0}"],
                remote: [[path: $/\/saml2p:Response\/saml2:Assertion\/saml2:Subject\/saml2:NameID\/@SPProvidedID/$]])

        and: "a saml:response with a value at the path specified by the mapping policy"
        def samlIssuer = generateUniqueIssuer()
        def attribName = "Frodo Baggins"
        def attribRole = "nova:cool_cat"
        def attribDomain = "193083"
        def attribEmail = "frodo.baggins@nowhere.abc"
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(
                issuer: samlIssuer,
                name: attribName,
                attributes: [roles: [attribRole], domain: [attribDomain], email: [attribEmail]],
                spProvidedId: extAttribPathValue,
                fakeSign: true))

        and: "an Identity mock that will return the mapping policy"
        String idpId = generateUniqueIdpId()
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(id: idpId)
        fakeIdentityV2Service.getMappingPolicyForIdpHandler = fakeIdentityV2Service
                .createGetMappingPolicyForIdp(mappings: [(idpId): mappingPolicy])

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        and: "the saml:response received by the origin service is unmarshalled"
        def response = samlUtilities.unmarshallResponse(mc.handlings[0].request.body as String)
        def attributes = response.assertions[0].attributeStatements[0].attributes

        then: "the request has two assertions"
        response.assertions.size() == 2

        and: "the default attributes are set correctly"
        attributes.find { it.name == "roles" }.attributeValues[0].value == attribRole
        attributes.find { it.name == "domain" }.attributeValues[0].value == attribDomain
        attributes.find { it.name == "email" }.attributeValues[0].value == attribEmail

        and: "the extended attributes are set correctly"
        attributes.find { it.name == "user/$extAttribLiteral" as String }.attributeValues[0].value == extAttribLiteralValue
        attributes.find { it.name == "user/$extAttribPath" as String }.attributeValues[0].value == extAttribPathValue
    }

    def "the access response (JSON) from the origin service will be translated before being sent to the client"() {
        given: "a mapping policy with a literal value and a path-based value in addition to the standard attributes"
        def extAttribLiteral = "potato"
        def extAttribLiteralValue = "salad"
        def extAttribPath = "pie"
        def extAttribPathValue = "eye"
        def mappingPolicy = createMappingJsonWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): "{0}"],
                remote: [[path: $/\/saml2p:Response\/saml2:Assertion\/saml2:Subject\/saml2:NameID\/@SPProvidedID/$]])

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
                .createGetMappingPolicyForIdp(mappings: [(idpId): mappingPolicy])

        when: "a request is sent to Repose requesting a JSON response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED, (ACCEPT): CONTENT_TYPE_JSON],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client receives JSON successfully"
        mc.receivedResponse.code as Integer == SC_OK
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_JSON

        when: "the response sent to the client is parsed into JSON"
        def json = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the extended attributes are set correctly"
        json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribLiteral" == extAttribLiteralValue
        json.access.'RAX-AUTH:extendedAttributes'.user."$extAttribPath" == extAttribPathValue
    }

    def "the access response (XML) from the origin service will be translated before being sent to the client"() {
        given: "a mapping policy with a fixed value and a dynamic value in addition to the standard attributes"
        def extAttribLiteral = "blues"
        def extAttribLiteralValue = "clues"
        def extAttribPath = "Swiper"
        def extAttribPathValue = "no swiping plz"
        def mappingPolicy = createMappingJsonWithValues(
                userExtAttribs: [(extAttribLiteral): extAttribLiteralValue, (extAttribPath): "{0}"],
                remote: [[path: $/\/saml2p:Response\/saml2:Assertion\/saml2:Subject\/saml2:NameID\/@SPProvidedID/$]])

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
                .createGetMappingPolicyForIdp(mappings: [(idpId): mappingPolicy])

        when: "a request is sent to Repose requesting an XML response"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED, (ACCEPT): CONTENT_TYPE_XML],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the client receives XML successfully"
        mc.receivedResponse.code as Integer == SC_OK
        mc.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == CONTENT_TYPE_XML

        when: "the response sent to the client is parsed into XML"
        def access = xmlSlurper.parseText(mc.receivedResponse.body as String)

        and: "we get the 'user' group in the extended attributes"
        def userGroup = access.'RAX-AUTH:extendedAttributes'.'*'.find { it.@name == "user" }

        then: "the extended attributes are set correctly"
        userGroup.'*'.find { it.@name == extAttribLiteral }.value[0].text() == extAttribLiteralValue
        userGroup.'*'.find { it.@name == extAttribPath }.value[0].text() == extAttribPathValue
    }
}
