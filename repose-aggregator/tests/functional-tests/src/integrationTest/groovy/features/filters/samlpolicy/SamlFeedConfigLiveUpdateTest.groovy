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
import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.PortFinder

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED

/**
 * This functional test exercises the config reloading of the filter.
 */
class SamlFeedConfigLiveUpdateTest extends ReposeValveTest {
    static final int FEED_POLLING_FREQUENCY_SEC = 1
    static final int FEED_POLLING_FREQUENCY_MILLIS = FEED_POLLING_FREQUENCY_SEC * 1_000
    static final String ATOM_FEED_LOG_SEARCH_STRING = "</atom:entry>"

    static final int REPOSE_START_WAIT_SEC = 30
    static final int CONFIG_REFRESH_WAIT_SEC = 15
    static final String CONFIG_DIR_SRC = "features/filters/samlpolicy/feedconfigliveupdate"

    static final int AKKA_CACHE_TIMEOUT_MILLIS = 500

    static MockIdentityV2Service fakeIdentityV2Service
    static AtomFeedResponseSimulator fakeAtomFeed
    static Endpoint atomEndpointOne
    static Endpoint atomEndpointTwo
    static Map params

    def setup() {
        reposeLogSearch.cleanLog()
        acquireFreshNewPorts()

        params = properties.defaultTemplateParams + [feedPollingFrequency: FEED_POLLING_FREQUENCY_SEC]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs(CONFIG_DIR_SRC, params)

        deproxy = new Deproxy()

        fakeAtomFeed = new AtomFeedResponseSimulator(properties.atomPort)
        atomEndpointOne = deproxy.addEndpoint(properties.atomPort, 'atom service 1', null, fakeAtomFeed.handler)
        atomEndpointTwo = deproxy.addEndpoint(properties.atomPort2, 'atom service 2', null, fakeAtomFeed.handler)

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        fakeIdentityV2Service.with {
            admin_token = UUID.randomUUID().toString()
            resetCounts()
        }
    }

    def "when no atom feed is initially configured but one gets configured while Repose is running, the filter should start reading from the feed"() {
        given: "we start Repose with no atom feed configured for the SAML filter"
        repose.configurationProvider.applyConfigs("$CONFIG_DIR_SRC/nofeed", params)
        repose.start(killOthersBeforeStarting: false, waitOnJmxAfterStarting: false)
        waitForReposeToStart()

        and: "this unique issuer will be used when generating each saml:response"
        def samlIssuer = generateUniqueIssuer()

        and: "an atom feed entry will be received with the same issuer as the saml:response issuer at some future point"
        def atomFeedEntry = fakeAtomFeed.atomEntryForIdpUpdate(issuer: samlIssuer)
        def atomFeedHandlerWithEntry = fakeAtomFeed.handlerWithEntry(atomFeedEntry)

        when: "we send our first request for this issuer"
        def mc = sendSamlRequest(samlIssuer)

        then: "the IDP and Mapping Policy endpoints are called"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "we wait for the akka cache to time out and reset the mock call counts"
        sleep(AKKA_CACHE_TIMEOUT_MILLIS)
        fakeIdentityV2Service.resetCounts()

        and: "another request is sent"
        mc = sendSamlRequest(samlIssuer)

        then: "the IDP and Mapping Policy endpoints are not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "the atom feed entry is made available and we give Repose a chance to have a nibble"
        atomEndpointOne.defaultHandler = atomFeedHandlerWithEntry
        atomEndpointTwo.defaultHandler = atomFeedHandlerWithEntry
        sleep(FEED_POLLING_FREQUENCY_MILLIS * 3)

        and: "the atom feed entries are made unavailable"
        atomEndpointOne.defaultHandler = fakeAtomFeed.handler
        atomEndpointTwo.defaultHandler = fakeAtomFeed.handler

        and: "a request is sent"
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest(samlIssuer)

        then: "the IDP and Mapping Policy endpoints are not called again"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "we update the filter's configuration to add an atom feed and"
        repose.configurationProvider.applyConfigs("$CONFIG_DIR_SRC/feedone", params)

        then:
        waitForReposeToLoadConfiguration()

        when: "a request is sent"
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest(samlIssuer)

        then: "the IDP and Mapping Policy endpoints are not called again indicating the cache is still intact"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 0
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 0

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]

        when: "the atom feed entry is made available and we wait until Repose logs that it processed the entry"
        atomEndpointOne.defaultHandler = atomFeedHandlerWithEntry
        reposeLogSearch.awaitByString(ATOM_FEED_LOG_SEARCH_STRING, 1, FEED_POLLING_FREQUENCY_SEC + 1)

        and: "a request is sent after the cache entry is supposed to be invalidated"
        atomEndpointOne.defaultHandler = fakeAtomFeed.handler
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest(samlIssuer)

        then: "the IDP and Mapping Policy endpoints are called again indicating the cache entry was invalidated"
        fakeIdentityV2Service.getGetIdpFromIssuerCount() == 1
        fakeIdentityV2Service.getGetMappingPolicyForIdpCount() == 1

        and: "the request is overall successful"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings[0]
    }

    def sendSamlRequest(String samlIssuer) {
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
    }

    def waitForReposeToStart() {
        reposeLogSearch.awaitByString("Repose ready", 1, REPOSE_START_WAIT_SEC)
    }

    def waitForReposeToLoadConfiguration() {
        reposeLogSearch.awaitByString("Configuration Updated:", 1, CONFIG_REFRESH_WAIT_SEC)
    }

    def acquireFreshNewPorts() {
        properties.reposePort = PortFinder.Singleton.getNextOpenPort()
        properties.atomPort = PortFinder.Singleton.getNextOpenPort()
        properties.atomPort2 = PortFinder.Singleton.getNextOpenPort()
        properties.targetPort = PortFinder.Singleton.getNextOpenPort()
        properties.identityPort = PortFinder.Singleton.getNextOpenPort()
    }
}
