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

import java.util.concurrent.TimeUnit

@Category(Slow.class)
class AtomFeedServiceConnectionPoolTest extends ReposeValveTest {

    Endpoint originEndpoint
    Endpoint atomEndpoint
    MockIdentityV2Service fakeIdentityV2Service
    AtomFeedResponseSimulator fakeAtomFeed

    def startReposeWithConfigParams(Map testParams = [:]) {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        Map params = properties.defaultTemplateParams + testParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/atomfeed", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "when an unauthenticated atom feed is not configured with a connection pool, it uses the default one"() {
        given: "keystone is configured to use the unauth atom feed with no connection pool configured"
        startReposeWithConfigParams("atom-feed-id": "unauth-feed-no-pool")

        and: "an atom feed entry is available for consumption"
        String atomFeedEntry = fakeAtomFeed.createAtomEntry(id: 'urn:uuid:101')
        HeaderCollection requestHeaders = new HeaderCollection()
        def headerGrabber = { Request request -> requestHeaders = request.headers }
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries([atomFeedEntry], headerGrabber)

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("</atom:entry>", 1, 11, TimeUnit.SECONDS)
        atomEndpoint.defaultHandler = fakeAtomFeed.handler

        then: "the atom feed entry is read by the Keystone V2 filter"
        reposeLogSearch.searchByString("<atom:id>urn:uuid:101</atom:id>").size() == 1

        and: "the default connection pool was used"
        requestHeaders.getFirstValue("X-Pool-Id") == "default-pool"
    }
}
