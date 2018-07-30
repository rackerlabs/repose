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

import features.filters.keystonev2.AtomFeedResponseSimulator
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import scaffold.category.Intense
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED
import static org.openrepose.framework.test.util.saml.SamlPayloads.*
import static org.openrepose.framework.test.util.saml.SamlUtilities.*

/**
 * This functional test exercises the cache feed invalidation.
 */
@Category(Intense.class)
class SamlCacheFeedInvalidationTest extends ReposeValveTest {
    static final int FEED_POLLING_FREQUENCY_SEC = 2
    static final String ATOM_FEED_LOG_SEARCH_STRING = "</atom:entry>"
    static final String SERVICE_CODE = "CloudIdentity"
    static final String RESOURCE_TYPE = "IDP"

    static MockIdentityV2Service fakeIdentityV2Service
    static AtomFeedResponseSimulator fakeAtomFeed
    static Endpoint atomEndpoint

    def setupSpec() {
        def params = properties.defaultTemplateParams + [feedPollingFrequency: FEED_POLLING_FREQUENCY_SEC]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/feedinvalidation", params)

        deproxy = new Deproxy()

        fakeAtomFeed = new AtomFeedResponseSimulator(properties.atomPort)
        atomEndpoint = deproxy.addEndpoint(properties.atomPort, 'atom service', null, fakeAtomFeed.handler)

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)
    }

    def setup() {
        fakeIdentityV2Service.resetCounts()
        reposeLogSearch.cleanLog()
    }

    @Unroll
    def "when an atom feed entry with serviceCode 'CloudIdentity', resourceType 'IDP', and type '#eventType' is received for an issuer, the mapping policy is removed from the cache"() {
        given: "the same issuer will be used to generate each unique saml:response"
        def samlIssuer = generateUniqueIssuer()
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED]

        and: "an atom feed entry will be received with the same issuer as the saml:response contains"
        def atomFeedEntry = fakeAtomFeed.atomEntryForIdpUpdate(eventType: eventType, issuer: samlIssuer)
        def atomFeedHandlerWithEntry = fakeAtomFeed.handlerWithEntry(atomFeedEntry)

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

        when: "the atom feed entry is made available and we wait until Repose logs that it processed the entry"
        atomEndpoint.defaultHandler = atomFeedHandlerWithEntry
        reposeLogSearch.awaitByString(ATOM_FEED_LOG_SEARCH_STRING, 1, FEED_POLLING_FREQUENCY_SEC + 1, TimeUnit.SECONDS)

        and: "a request is sent after the cache entry is supposed to be invalidated"
        atomEndpoint.defaultHandler = fakeAtomFeed.handler
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

        where:
        eventType << ["CREATE", "UPDATE", "DELETE", "Surprise"]
    }

    def "when an atom feed is received with multiple entries containing multiple issuers, the correct mapping policies are removed from the cache"() {
        given: "a list of issuers that will be used in the saml:responses"
        def samlIssuers = (1..5).collect { generateUniqueIssuer() }
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED]

        and: "an atom feed will be received with entries containing the same issuers plus one more"
        def atomFeedHandlerWithEntries = fakeAtomFeed.handlerWithEntries(
                (samlIssuers + generateUniqueIssuer()).collect { fakeAtomFeed.atomEntryForIdpUpdate(issuer: it) })

        when: "requests are sent for each issuer for the first time"
        def mcs = samlIssuers.collect { samlIssuer ->
            def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
            def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
            deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)
        }

        then: "the IDP and Mapping Policy endpoints are called for each issuer"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == samlIssuers.size()
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == samlIssuers.size()

        and: "the requests are all successful"
        mcs.every { it.receivedResponse.code as Integer == SC_OK }
        mcs.every { it.handlings[0] }

        when: "requests are sent again immediately after the first round of requests"
        fakeIdentityV2Service.resetCounts()
        mcs = samlIssuers.collect { samlIssuer ->
            def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
            def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
            deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)
        }

        then: "the IDP and Mapping Policy endpoints are not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the requests are all successful"
        mcs.every { it.receivedResponse.code as Integer == SC_OK }
        mcs.every { it.handlings[0] }

        when: "the atom feed entry is made available and we wait until Repose logs that it processed the entries plus one more"
        atomEndpoint.defaultHandler = atomFeedHandlerWithEntries
        reposeLogSearch.awaitByString(ATOM_FEED_LOG_SEARCH_STRING, samlIssuers.size() + 1, FEED_POLLING_FREQUENCY_SEC + 1, TimeUnit.SECONDS)

        and: "requests are sent after the cache entries are supposed to be invalidated"
        atomEndpoint.defaultHandler = fakeAtomFeed.handler
        fakeIdentityV2Service.resetCounts()
        mcs = samlIssuers.collect { samlIssuer ->
            def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
            def requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
            deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)
        }

        then: "the IDP and Mapping Policy endpoints are called again for each issuer"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == samlIssuers.size()
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == samlIssuers.size()

        and: "the requests are all successful"
        mcs.every { it.receivedResponse.code as Integer == SC_OK }
        mcs.every { it.handlings[0] }
    }

    @Unroll
    def "when an atom feed entry with serviceCode '#serviceCode' and resourceType '#resourceType' is received for the same issuer (#issuerMatch), the mapping policy is NOT removed from the cache"() {
        given: "the same issuer will be used to generate each unique saml:response"
        def samlIssuer = generateUniqueIssuer()
        def url = reposeEndpoint + SAML_AUTH_URL
        def headers = [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED]

        and: "an atom feed entry will be received with the specific values for the specific test"
        def atomFeedEntry = fakeAtomFeed.atomEntryForIdpUpdate(
                serviceCode: serviceCode, resourceType: resourceType, issuer: issuerMatch ? samlIssuer : "pineapple")
        def atomFeedHandlerWithEntry = fakeAtomFeed.handlerWithEntry(atomFeedEntry)

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

        when: "the atom feed entry is made available and we wait until Repose logs that it processed the entry"
        atomEndpoint.defaultHandler = atomFeedHandlerWithEntry
        reposeLogSearch.awaitByString(ATOM_FEED_LOG_SEARCH_STRING, 1, FEED_POLLING_FREQUENCY_SEC + 1, TimeUnit.SECONDS)

        and: "a request is sent after the cache entry is supposed to be invalidated"
        atomEndpoint.defaultHandler = fakeAtomFeed.handler
        fakeIdentityV2Service.resetCounts()
        saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))
        requestBody = asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml))
        mc = deproxy.makeRequest(url: url, method: HTTP_POST, headers: headers, requestBody: requestBody)

        then: "the IDP and Mapping Policy endpoints are still not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        where:
        serviceCode     | resourceType  | issuerMatch
        "RandomService" | RESOURCE_TYPE | true
        SERVICE_CODE    | "POTATO_FARM" | true
        SERVICE_CODE    | RESOURCE_TYPE | false
    }
}
