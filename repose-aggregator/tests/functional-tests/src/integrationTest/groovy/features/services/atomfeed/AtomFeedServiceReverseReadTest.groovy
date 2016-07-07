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

import java.util.concurrent.TimeUnit

@Category(Slow.class)
class AtomFeedServiceReverseReadTest extends ReposeValveTest {
    Endpoint originEndpoint
    Endpoint atomEndpoint
    MockIdentityV2Service fakeIdentityV2Service
    AtomFeedResponseSimulator fakeAtomFeed

    def setup() {
        deproxy = new Deproxy()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        Map params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/atom", params)
        repose.configurationProvider.applyConfigs("features/services/atomfeed/reverseread", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        waitUntilReadyToServiceRequests()
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "when an atom feed entry is received, it is passed to the filter"() {
        given: "there is an atom feed entry available for consumption"
        String atomFeedEntry = fakeAtomFeed.createAtomEntry(id: 'urn:uuid:101')
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries([atomFeedEntry])

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:101</atom:id>", 1, 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entry"
        atomFeedEntry.eachLine { line ->
            assert reposeLogSearch.searchByString(line.trim()).size() == 1
            true
        }
    }

    def "when multiple atom feed entries are received, they are passed in reverse-read order to the filter"() {
        given: "there is a list of atom feed entries available for consumption"
        List<String> ids = (201..210).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                ids.collect { fakeAtomFeed.createAtomEntry(id: "urn:uuid:$it") })

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:2\\d{2}</atom:id>", ids.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def logLines = reposeLogSearch.searchByString("<atom:id>urn:uuid:2\\d{2}</atom:id>")
        logLines.size() == ids.size()
        logLines.collect { (it =~ /\s*<atom:id>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == ids.reverse()
    }

    def "when multiple pages of atom feed entries are received, they are all processed by the filter in the correct reverse-read order"() {
        given: "there is a list of atom feed entries available for consumption"
        List<String> ids = (301..325).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                ids.collect { fakeAtomFeed.createAtomEntry(id: "urn:uuid:$it") })

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:3\\d{2}</atom:id>", ids.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def logLines = reposeLogSearch.searchByString("<atom:id>urn:uuid:3\\d{2}</atom:id>")
        logLines.size() == ids.size()
        logLines.collect { (it =~ /\s*<atom:id>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == ids.reverse()

        when: "there are more entries on the next page"
        def moreIds = (401..425).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                moreIds.collect { fakeAtomFeed.createAtomEntry(id: "urn:uuid:$it") })

        and: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString("<atom:id>urn:uuid:4\\d{2}</atom:id>", moreIds.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def moreLogLines = reposeLogSearch.searchByString("<atom:id>urn:uuid:4\\d{2}</atom:id>")
        moreLogLines.size() == moreIds.size()
        moreLogLines.collect { (it =~ /\s*<atom:id>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == moreIds.reverse()
    }
}
