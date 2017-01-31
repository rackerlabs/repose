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
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * This functional test verifies that the cache can be turned off.
 */
class SamlCacheDisabledTest extends ReposeValveTest {
    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/nocache", params)

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
    }

    def "when the same saml:response is sent multiple times (same Issuer), the mapping policy should be retrieved from Identity each time"() {
        given: "the same saml:response will be used in each request"
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED]
        def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(SAML_ONE_ASSERTION_SIGNED))

        and: "we will make several requests"
        def numOfRequests = 22

        when:
        def mcs = (1..numOfRequests).collect {
            deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)
        }

        then: "the client gets back a good response every time"
        mcs.every { it.receivedResponse.code as Integer == SC_OK }

        and: "the origin service received the request every time"
        mcs.every { it.handlings[0] }
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == numOfRequests

        and: "the IDP and Mapping Policy endpoints were called every time"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == numOfRequests
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == numOfRequests
    }

    def "when multiple unique saml:responses are sent with the same Issuer, the mapping policy should be retrieved from Identity each time"() {
        given: "the same issuer will be used to generate each unique saml:response"
        def samlIssuer = generateUniqueIssuer()
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): CONTENT_TYPE_FORM_URLENCODED]

        and: "we will make several requests"
        def numOfRequests = 21

        when:
        def mcs = (1..numOfRequests).collect {
            def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
            def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
            deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)
        }

        then: "the client gets back a good response every time"
        mcs.every { it.receivedResponse.code as Integer == SC_OK }

        and: "the origin service received the request every time"
        mcs.every { it.handlings[0] }
        fakeIdentityV2Service.getGenerateTokenFromSamlResponseCount() == numOfRequests

        and: "the IDP and Mapping Policy endpoints were called every time"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == numOfRequests
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == numOfRequests
    }
}
