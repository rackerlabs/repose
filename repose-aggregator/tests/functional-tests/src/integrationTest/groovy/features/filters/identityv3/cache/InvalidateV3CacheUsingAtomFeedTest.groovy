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
package features.filters.identityv3.cache

import features.filters.keystonev2.AtomFeedResponseSimulator
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

import java.util.concurrent.TimeUnit

@Category(Filters)
class InvalidateV3CacheUsingAtomFeedTest extends ReposeValveTest {
    Endpoint originEndpoint
    Endpoint atomEndpoint
    Endpoint identityEndpoint
    AtomFeedResponseSimulator fakeAtomFeed
    MockIdentityV3Service fakeIdentityV3Service

    def setup() {
        deproxy = new Deproxy()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/atom", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "when token is cached then invalidated by atom feed, should attempt to re-validate token with identity endpoint"() {
        when: "I send a GET request to REPOSE with an X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 1
        mc.handlings[0].endpoint == originEndpoint

        when: "I send a GET request to REPOSE with the same X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 0
        mc.handlings[0].endpoint == originEndpoint

        when: "identity atom feed has an entry that should invalidate the tenant associated with this X-Subject-Token"
        fakeIdentityV3Service.with {
            fakeIdentityV3Service.validateTokenHandler = { tokenId, request -> new Response(404) }
        }
        fakeIdentityV3Service.resetCounts()
        fakeAtomFeed.hasEntry = true
        atomEndpoint.defaultHandler = fakeAtomFeed.handler

        and: "we wait for repose to process the atom feed entry"
        reposeLogSearch.awaitByString(
            "OpenStackIdentityV3Filter - Processing atom feed entry",
            1,
            11,
            TimeUnit.SECONDS
        )

        and: "I send a GET request to REPOSE with the same X-Subject-Token header"
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token],
                defaultHandler: fakeIdentityV3Service.handler)

        then: "Repose should not have the token in the cache any more, so it try to validate it, which will fail and result in a 401"
        mc.receivedResponse.code == '401'
        mc.handlings.size() == 0
        fakeIdentityV3Service.validateTokenCount == 1
    }

    def "When a user is cached by repose, and a user Update event, invalidate the cache for that user"() {
        when: "I send a GET request to REPOSE with an X-Subject-Token header for a specific user"
        fakeIdentityV3Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 1
        fakeIdentityV3Service.getGroupsCount == 1
        mc.handlings[0].endpoint == originEndpoint

        when: "I send a GET request to REPOSE with the same X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 0
        fakeIdentityV3Service.getGroupsCount == 0
        mc.handlings[0].endpoint == originEndpoint

        when: "Identity atom feed has a Update User Event"
        //Identity needs to respond normally, so that we can get "new" user info
        fakeIdentityV3Service.resetCounts()
        fakeAtomFeed.hasEntry = true
        atomEndpoint.defaultHandler = fakeAtomFeed.userUpdateHandler(fakeIdentityV3Service.client_userid.toString())

        and: "we wait for repose to process the atom feed entry"
        reposeLogSearch.awaitByString(
            "OpenStackIdentityV3Filter - Processing atom feed entry",
            1,
            11,
            TimeUnit.SECONDS
        )

        and: "I send a GET request to REPOSE with the same X-Subject-Token header"
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should not have the token in the cache any more, so it shoudl try to re-validate it"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 1
        fakeIdentityV3Service.getGroupsCount == 1
    }

    def "When a user is cached by repose, and a TRR event comes in, invalidate all of the cache for that user"() {
        when: "I send a GET request to REPOSE with an X-Subject-Token header for a specific user"
        fakeIdentityV3Service.resetCounts()
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 1
        mc.handlings[0].endpoint == originEndpoint

        when: "I send a GET request to REPOSE with the same X-Subject-Token header"
        fakeIdentityV3Service.resetCounts()
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityV3Service.validateTokenCount == 0
        mc.handlings[0].endpoint == originEndpoint

        when: "Identity atom feed has a TRR event to invalidate our user"
        fakeIdentityV3Service.with {
            fakeIdentityV3Service.validateTokenHandler = { tokenId, request -> new Response(404) }
        }
        fakeIdentityV3Service.resetCounts()
        fakeAtomFeed.hasEntry = true
        atomEndpoint.defaultHandler = fakeAtomFeed.trrEventHandler(fakeIdentityV3Service.client_userid.toString())

        and: "we wait for repose to process the atom feed entry"
        reposeLogSearch.awaitByString(
            "OpenStackIdentityV3Filter - Processing atom feed entry",
            1,
            11,
            TimeUnit.SECONDS
        )

        and: "I send a GET request to REPOSE with the same X-Subject-Token header"
        mc = deproxy.makeRequest(
                url: reposeEndpoint,
                method: 'GET',
                headers: ['X-Subject-Token': fakeIdentityV3Service.client_token],
                defaultHandler: fakeIdentityV3Service.handler)

        then: "Repose should not have the token in the cache any more, so it try to validate it, which will fail and result in a 401"
        mc.receivedResponse.code == '401'
        mc.handlings.size() == 0
        fakeIdentityV3Service.validateTokenCount == 1
    }
}
