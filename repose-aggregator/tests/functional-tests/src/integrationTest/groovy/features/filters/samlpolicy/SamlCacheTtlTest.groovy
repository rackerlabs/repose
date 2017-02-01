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
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED

/**
 * This functional test exercises the cache TTL expiration.
 */
class SamlCacheTtlTest extends ReposeValveTest {
    static final int CACHE_TTL_SEC = 2
    static final int CACHE_TTL_MILLIS = CACHE_TTL_SEC * 1_000
    static final int SLEEP_PADDING_MILLIS = 500

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams + [cacheTtl: CACHE_TTL_SEC]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/cachettl", params)

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

    def "when the same saml:response is sent after the cache entry is supposed to expire, the mapping policy should be retrieved from Identity again"() {
        given: "the same saml:response will be used in each request"
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED]
        def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(SAML_ONE_ASSERTION_SIGNED))

        when: "a request is sent for the first time"
        def mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are called"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "a request is sent again immediately after the first request"
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "a request is sent after the cache entry is supposed to expire"
        sleep(CACHE_TTL_MILLIS + SLEEP_PADDING_MILLIS)
        fakeIdentityV2Service.resetCounts()
        mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]
    }

    def "when a saml:response with the same Issuer as a previous one is sent after the cache entry is supposed to expire, the mapping policy should be retrieved from Identity again"() {
        given: "the same saml:response will be used in each request"
        def samlIssuer = generateUniqueIssuer()
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED]

        when: "a request is sent for the first time"
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
        def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
        def mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are called"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "a request is sent again immediately after the first request"
        fakeIdentityV2Service.resetCounts()
        saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
        requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
        mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "a request is sent after the cache entry is supposed to expire"
        sleep(CACHE_TTL_MILLIS + SLEEP_PADDING_MILLIS)
        fakeIdentityV2Service.resetCounts()
        saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
        requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
        mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]
    }
}
