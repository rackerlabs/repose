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
import framework.ReposeValveTest
import framework.category.Slow
import framework.mocks.MockIdentityV2Service
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.HeaderCollection
import org.rackspace.deproxy.Request
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

@Category(Slow.class)
class AtomFeedServiceConnectionPoolTest extends ReposeValveTest {

    Endpoint originEndpoint
    Endpoint atomEndpoint
    MockIdentityV2Service fakeIdentityV2Service
    AtomFeedResponseSimulator fakeAtomFeed

    def startReposeWithConfigParams(Map testParams = [:]) {
        deproxy = new Deproxy()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        Map params = properties.defaultTemplateParams + testParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/atomfeed", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    @Unroll
    def "when an #feedType atom feed is configured with connection pool #connectionPool, it uses the #expectedPoolId pool"() {
        given: "keystone is configured to use the unauth atom feed with no connection pool configured"
        startReposeWithConfigParams("atom-feed-id": atomFeedId)

        and: "an atom feed entry is available for consumption"
        String atomFeedEntry = fakeAtomFeed.createAtomEntry(id: "urn:uuid:$entryId")
        def atomFeedHandler = fakeAtomFeed.handlerWithEntries([atomFeedEntry])
        HeaderCollection requestHeaders = new HeaderCollection()
        def atomFeedHandlerWrapper = { Request request ->
            requestHeaders = request.headers
            atomFeedHandler(request)
        }
        atomEndpoint.defaultHandler = atomFeedHandlerWrapper

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:$entryId</atom:id>", 2, 6, TimeUnit.SECONDS)

        then: "the correct connection pool was used"
        requestHeaders.getFirstValue("X-Pool-Id") == expectedPoolId

        where:
        feedType          | connectionPool          | atomFeedId                 | expectedPoolId          | entryId
        "unauthenticated" | "<none>"                | "unauth-feed-no-pool"      | "default-pool"          | 103
        "unauthenticated" | "atom-feed-pool-unauth" | "unauth-feed-pool"         | "atom-feed-pool-unauth" | 105
        "unauthenticated" | "missing-pool-unauth"   | "unauth-feed-missing-pool" | "default-pool"          | 107
        "authenticated"   | "<none>"                | "auth-feed-no-pool"        | "default-pool"          | 109
        "authenticated"   | "atom-feed-pool-auth"   | "auth-feed-pool"           | "atom-feed-pool-auth"   | 111
        "authenticated"   | "missing-pool-auth"     | "auth-feed-missing-pool"   | "default-pool"          | 113
    }
}
