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

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static framework.mocks.MockIdentityV2Service.isSamlIdpIssuerCallPath
import static framework.mocks.MockIdentityV2Service.isSamlIdpMappingPolicyCallPath
import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED

/**
 * This functional test verifies the plain text version of the tracing header can be added to the connections being
 * made from this filter.
 */
class SamlTracingPlainHeaderTest extends ReposeValveTest {
    static final String TRACING_PLAIN_HEADER = "X-Request-Id"

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/tracingplain", params)

        deproxy = new Deproxy()

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
    }

    def "the call to Identity to generate an admin token includes the plain tracing header"() {
        given: "a new token in the mock which will force the filter to re-request a token if it already has"
        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()

        when: "a request is sent to Repose"
        def mc = sendSamlRequestWithUniqueIssuer()

        and: "we look for orphaned handlings matching the generate token endpoint"
        def adminHandlings = mc.orphanedHandlings.findAll { it.request.path.contains("/v2.0/tokens") && it.request.method == "POST" }

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the generate token endpoint was called once"
        adminHandlings.size() == 1

        and: "the generate token call includes the plain tracing header"
        adminHandlings[0].request.headers.getCountByName(TRACING_PLAIN_HEADER) == 1

        and: "the generate token call's plain tracing header matches the one sent to the origin service"
        adminHandlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER) == mc.handlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER)
    }

    def "the call to Identity to get the IDP ID for a given Issuer includes the plain tracing header"() {
        when: "a request is sent to Repose"
        def mc = sendSamlRequestWithUniqueIssuer()

        and: "we look for orphaned handlings matching the Issuer to IDP ID endpoint"
        def issuerHandlings = mc.orphanedHandlings.findAll { isSamlIdpIssuerCallPath(it.request.path) && it.request.method == "GET" }

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the Issuer to IDP ID endpoint was called once"
        issuerHandlings.size() == 1

        and: "the Issuer to IDP ID call includes the plain tracing header"
        issuerHandlings[0].request.headers.getCountByName(TRACING_PLAIN_HEADER) == 1

        and: "the Issuer to IDP ID call's plain tracing header matches the one sent to the origin service"
        issuerHandlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER) == mc.handlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER)
    }

    def "the call to Identity to get the Mapping Policy for a given IDP ID includes the plain tracing header"() {
        when: "a request is sent to Repose"
        def mc = sendSamlRequestWithUniqueIssuer()

        and: "we look for orphaned handlings matching the Mapping Policy to Issuer endpoint"
        def mappingPolicyHandlings = mc.orphanedHandlings.findAll { isSamlIdpMappingPolicyCallPath(it.request.path) && it.request.method == "GET" }

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the Mapping Policy to Issuer endpoint was called once"
        mappingPolicyHandlings.size() == 1

        and: "the Mapping Policy to Issuer call includes the plain tracing header"
        mappingPolicyHandlings[0].request.headers.getCountByName(TRACING_PLAIN_HEADER) == 1

        and: "the Mapping Policy to Issuer call's plain tracing header matches the one sent to the origin service"
        mappingPolicyHandlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER) == mc.handlings[0].request.headers.getFirstValue(TRACING_PLAIN_HEADER)
    }

    def sendSamlRequestWithUniqueIssuer() {
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
    }
}
