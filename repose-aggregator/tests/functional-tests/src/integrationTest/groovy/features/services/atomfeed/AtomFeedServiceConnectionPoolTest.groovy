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

package features.services.atomfeed

import features.filters.keystonev2.AtomFeedResponseSimulator
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.HeaderCollection
import org.rackspace.deproxy.Request
import scaffold.category.Services
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

@Category(Services)
class AtomFeedServiceConnectionPoolTest extends ReposeValveTest {

    Endpoint atomEndpoint
    Endpoint identityEndpoint
    MockIdentityV2Service fakeIdentityV2Service
    AtomFeedResponseSimulator fakeAtomFeed
    HeaderCollection requestHeadersToIdentity = null

    def setup() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        def identityHandlerWrapper = { Request request ->
            requestHeadersToIdentity = request.headers
            fakeIdentityV2Service.handler(request)
        }
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, identityHandlerWrapper)

        deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def startReposeWithConfigParams(Map testParams = [:]) {
        Map params = properties.defaultTemplateParams + testParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/atomfeed", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "when an #feedType atom feed is configured with connection pool #connectionPool, it uses the #expectedPoolId pool"() {
        given: "Keystone is configured to use the correct atom feed with the specified connection pool"
        startReposeWithConfigParams("atom-feed-id": atomFeedId)

        and: "we provide a valid atom feed entry with a handler that will capture the request headers"
        def atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$entryId")
        def atomFeedHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)
        HeaderCollection requestHeadersToAtomFeed = null
        def atomFeedHandlerWrapper = { Request request ->
            requestHeadersToAtomFeed = request.headers
            atomFeedHandler(request)
        }
        atomEndpoint.defaultHandler = atomFeedHandlerWrapper

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:$entryId</atom:id>", 2, 6, TimeUnit.SECONDS)

        then: "the correct connection pool was used"
        requestHeadersToAtomFeed?.getFirstValue("X-Pool-Id") == expectedPoolId

        where:
        feedType          | connectionPool          | atomFeedId                 | expectedPoolId          | entryId
        "unauthenticated" | "<none>"                | "unauth-feed-no-pool"      | "default-pool"          | 103
        "unauthenticated" | "atom-feed-pool-unauth" | "unauth-feed-pool"         | "atom-feed-pool-unauth" | 105
        "unauthenticated" | "missing-pool-unauth"   | "unauth-feed-missing-pool" | "default-pool"          | 107
        "authenticated"   | "<none>"                | "auth-feed-no-pool"        | "default-pool"          | 109
        "authenticated"   | "atom-feed-pool-auth"   | "auth-feed-pool"           | "atom-feed-pool-auth"   | 111
        "authenticated"   | "missing-pool-auth"     | "auth-feed-missing-pool"   | "default-pool"          | 113
    }

    @Unroll
    def "when an authenticated atom feed is configured to #connectionPool, the authentication request uses the connection pool #expectedPoolId"() {
        given: "Keystone is configured to use the correct atom feed with the specified connection pool"
        startReposeWithConfigParams("atom-feed-id": atomFeedId)

        and: "an atom feed entry is available for consumption"
        def atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$entryId")
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:$entryId</atom:id>", 2, 6, TimeUnit.SECONDS)

        then: "the correct connection pool was used"
        requestHeadersToIdentity?.getFirstValue("X-Pool-Id") == expectedPoolId

        where:
        connectionPool        | atomFeedId               | expectedPoolId        | entryId
        "<none>"              | "auth-feed-no-pool"      | "default-pool"        | 215
        "atom-feed-pool-auth" | "auth-feed-pool"         | "atom-feed-pool-auth" | 217
        "missing-pool-auth"   | "auth-feed-missing-pool" | "default-pool"        | 219
    }

    @Unroll
    def "when an #feedType atom feed times out in the Akka Service Client, the next request will still work"() {
        given: "Keystone is configured to use the correct atom feed with the specified connection pool"
        startReposeWithConfigParams("atom-feed-id": atomFeedId)

        and: "an atom feed entry is available for consumption"
        def atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$entryId")
        def atomFeedHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)
        def atomFeedHandlerWrapper = { Request request ->
            sleep(2_000) // configured timeout is 1 second
            atomFeedHandler(request)
        }
        atomEndpoint.defaultHandler = atomFeedHandlerWrapper

        when: "the Akka Service Client times out"
        reposeLogSearch.awaitByString("java.net.SocketTimeoutException: Read timed out", 1, 5, TimeUnit.SECONDS)

        and: "we provide a valid atom feed entry for the next attempt with a handler that will capture the request headers"
        atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:${entryId + 4}")
        atomFeedHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)
        HeaderCollection requestHeadersToAtomFeed = null
        atomFeedHandlerWrapper = { Request request ->
            requestHeadersToAtomFeed = request.headers
            atomFeedHandler(request)
        }
        atomEndpoint.defaultHandler = atomFeedHandlerWrapper

        and: "we wait for the Keystone V2 filter to read the feed"
        // Repose should log receiving 325 and 327
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:${entryId + 4}</atom:id>", 2, 10, TimeUnit.SECONDS)

        then: "the correct connection pool was used"
        requestHeadersToAtomFeed?.getFirstValue("X-Pool-Id") == expectedPoolId

        and: "the timed out atom feed entry did not make it through"
        // Repose will never log receiving 321 and 323
        reposeLogSearch.searchByString("<atom:id>urn:uuid:$entryId</atom:id>").size() == 0

        where:
        feedType          | atomFeedId                 | expectedPoolId       | entryId
        "unauthenticated" | "unauth-feed-timeout-pool" | "small-timeout-pool" | 321
        "authenticated"   | "auth-feed-timeout-pool"   | "small-timeout-pool" | 323
    }

    def "when authentication takes too long, the atom feed service times out"() {
        given: "identity will take longer than the configured authentication timeout allows"
        identityEndpoint.defaultHandler = { Request request ->
            sleep(2_500) // configured timeout is 1.5 seconds
            fakeIdentityV2Service.handler(request)
        }

        and: "Keystone is configured to use the correct atom feed with the specified connection pool"
        startReposeWithConfigParams("atom-feed-id": "auth-feed-auth-timeout")

        when: "the authentication times out"
        reposeLogSearch.awaitByString("Futures timed out after \\[1500 milliseconds\\]", 1, 5, TimeUnit.SECONDS)

        and: "the identity handler is updated to not take so long"
        identityEndpoint.defaultHandler = fakeIdentityV2Service.handler

        and: "an atom feed entry is made available"
        def atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:429")
        def atomFeedHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)
        HeaderCollection requestHeadersToAtomFeed = null
        def atomFeedHandlerWrapper = { Request request ->
            requestHeadersToAtomFeed = request.headers
            atomFeedHandler(request)
        }
        atomEndpoint.defaultHandler = atomFeedHandlerWrapper

        and: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:429</atom:id>", 2, 6, TimeUnit.SECONDS)

        then: "the correct connection pool was used"
        requestHeadersToAtomFeed?.getFirstValue("X-Pool-Id") == "default-pool"
    }
}
