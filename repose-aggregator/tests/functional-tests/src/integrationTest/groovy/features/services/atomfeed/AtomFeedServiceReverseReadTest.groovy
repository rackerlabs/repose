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
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import scaffold.category.Services

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Services)
class AtomFeedServiceReverseReadTest extends ReposeValveTest {
    Endpoint originEndpoint
    Endpoint atomEndpoint
    MockIdentityV2Service fakeIdentityV2Service
    AtomFeedResponseSimulator fakeAtomFeed

    def setup() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        Map params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/atom", params)
        repose.configurationProvider.applyConfigs("features/services/atomfeed/reverseread", params)

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "when an atom feed entry is received, it is passed to the filter"() {
        given: "Repose has been started"
        startRepose()

        and: "there is an atom feed entry available for consumption"
        def atomFeedEntry = fakeAtomFeed.atomEntryForTokenInvalidation(id: 'urn:uuid:101')
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntry(atomFeedEntry)

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString(">urn:uuid:101</atom:id>", 1, 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entry"
        AtomFeedResponseSimulator.buildXmlToString(atomFeedEntry).eachLine { line ->
            assert reposeLogSearch.searchByString(line.trim()).size() == 1
            true
        }
    }

    def "when multiple atom feed entries are received, they are passed in reverse order to the filter"() {
        given: "Repose has been started"
        startRepose()

        and: "there is a list of atom feed entries available for consumption"
        List<String> ids = (201..210).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                ids.collect { fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$it") })

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString(">urn:uuid:2\\d{2}</atom:id>", ids.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def logLines = reposeLogSearch.searchByString(">urn:uuid:2\\d{2}</atom:id>")
        logLines.size() == ids.size()
        logLines.collect { (it =~ /\s*>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == ids.reverse()
    }

    def "when multiple pages of atom feed entries are received, they are all processed by the filter in reverse order"() {
        given: "Repose has been started"
        startRepose()

        and: "there is a list of atom feed entries available for consumption"
        List<String> ids = (301..325).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                ids.collect { fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$it") })

        when: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString(">urn:uuid:3\\d{2}</atom:id>", ids.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def logLines = reposeLogSearch.searchByString(">urn:uuid:3\\d{2}</atom:id>")
        logLines.size() == ids.size()
        logLines.collect { (it =~ /\s*>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == ids.reverse()

        when: "there are more entries on the next page"
        def moreIds = (401..425).collect {it as String}
        atomEndpoint.defaultHandler = fakeAtomFeed.handlerWithEntries(
                moreIds.collect { fakeAtomFeed.atomEntryForTokenInvalidation(id: "urn:uuid:$it") })

        and: "we wait for the Keystone V2 filter to read the feed"
        reposeLogSearch.awaitByString(">urn:uuid:4\\d{2}</atom:id>", moreIds.size(), 6, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the atom feed entries in order"
        def moreLogLines = reposeLogSearch.searchByString(">urn:uuid:4\\d{2}</atom:id>")
        moreLogLines.size() == moreIds.size()
        moreLogLines.collect { (it =~ /\s*>urn:uuid:(\d+)<\/atom:id>.*/)[0][1] } == moreIds.reverse()
    }

    def "when an event is published to an empty feed, it should be processed"() {
        given: "an event to be published after the first read"
        List<Map<String, String>> pages = [[[id: "urn:uuid:1"]]]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.atomFeedWithNoEntries()
                    break
                default:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed(pages)[0]
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 1, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == pages.flatten().collect { it.id }
    }

    def "when a single entry is published to a multi-page feed, it should be processed"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "new pages of a feed with a single new entry"
        List<List<Map<String, String>>> updatedPages = [
            (51..51).collect { [id: "urn:uuid:$it"] },
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    if (request.path.contains("urn:uuid:25")) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[2]
                    } else if ((request.path.contains("urn:uuid:50"))) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[1]
                    } else {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
                    }
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 1, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == updatedPages[0].collect { it.id }
    }

    def "when a single entry is published on an existing page of a multi-page feed, it should be processed"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (49..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "new pages of a feed with a single new entry"
        List<List<Map<String, String>>> updatedPages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 1, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == updatedPages[0].collect { it.id }.take(1)
    }

    def "when multiple entries are published to a multi-page feed, they should all be processed in reverse order"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a single new page of a feed with a multiple new entries"
        List<List<Map<String, String>>> updatedPages = [
            (55..51).collect { [id: "urn:uuid:$it"] },
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    if (request.path.contains("urn:uuid:25")) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[2]
                    } else if ((request.path.contains("urn:uuid:50"))) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[1]
                    } else {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
                    }
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 5, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == updatedPages[0].collect { it.id }.reverse()
    }

    def "when multiple pages are published to a multi-page feed, they should all be processed in reverse order"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "multiple new pages of a feed with a multiple new entries"
        List<List<Map<String, String>>> updatedPages = [
            (85..76).collect { [id: "urn:uuid:$it"] },
            (75..51).collect { [id: "urn:uuid:$it"] },
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    if (request.path.contains("urn:uuid:25")) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[3]
                    } else if ((request.path.contains("urn:uuid:50"))) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[2]
                    } else if ((request.path.contains("urn:uuid:75"))) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[1]
                    } else {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
                    }
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 35, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == (updatedPages[0] + updatedPages[1]).collect { it.id }.reverse()
    }

    def "after the high water mark entry falls off the feed, all new entries should be processed in reverse order"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "multiple new pages of a feed with a multiple new entries"
        List<List<Map<String, String>>> updatedPages = [
            (80..76).collect { [id: "urn:uuid:$it"] },
            (75..51).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    if (request.path.contains("urn:uuid:75")) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[1]
                    } else {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
                    }
            }
            new Response(SC_OK, null, fakeAtomFeed.headers, responseBody)
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 30, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == (updatedPages[0] + updatedPages[1]).collect { it.id }.reverse()
    }

    def "when the Atom feed response is chunked, all new entries should still be processed in reverse order"() {
        given: "existing pages of a feed before we read it"
        List<List<Map<String, String>>> pages = [
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "new pages of a feed with a single new entry"
        List<List<Map<String, String>>> updatedPages = [
            (51..51).collect { [id: "urn:uuid:$it"] },
            (50..26).collect { [id: "urn:uuid:$it"] },
            (25..1).collect { [id: "urn:uuid:$it"] },
        ]

        and: "a feed with events changing between reads"
        int timesRead = 0
        atomEndpoint.defaultHandler = { Request request ->
            String responseBody
            switch (timesRead++) {
                case 0:
                    responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], pages)[0]
                    break
                default:
                    if (request.path.contains("urn:uuid:25")) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[2]
                    } else if ((request.path.contains("urn:uuid:50"))) {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[1]
                    } else {
                        responseBody = fakeAtomFeed.pagedTokenInvalidationAtomFeed([pageSize: 25], updatedPages)[0]
                    }
            }
            new Response(SC_OK, null, fakeAtomFeed.headers + ["Transfer-Encoding": "chunked"], AtomFeedServiceTest.chunkString(responseBody))
        }

        when: "Repose has been started"
        startRepose()

        and: "we wait for the feed to be read after publishing an event"
        reposeLogSearch.awaitByString("</atom:entry>", 1, 11, TimeUnit.SECONDS)

        then: "the Keystone V2 filter logs receiving the Atom feed event"
        def logLines = reposeLogSearch.searchByString(">.*</atom:id>")
        logLines.collect { (it =~ /.*>(urn:uuid:\d+)<\/atom:id>.*/)[0][1] } == updatedPages[0].collect { it.id }
    }

    void startRepose() {
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }
}
