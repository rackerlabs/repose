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

import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

@Category(Filters)
class IdentityV3GroupCacheOffsetTest extends ReposeValveTest {
    // configured times
    private static final int GROUPS_TIMEOUT = 5_000
    private static final int CACHE_VARIANCE = 1_000

    def identityEndpoint
    MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/cacheoffset/groups", params)
        repose.start()
        waitUntilReadyToServiceRequests("401")
    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of GROUPS_TIMEOUT +/- CACHE_VARIANCE
     * - all groups will expire at GROUPS_TIMEOUT + CACHE_VARIANCE
     */
    def "should cache tokens using cache offset"() {
        given: "Identity Service returns cache tokens with 1 day expiration"
        def clientToken = UUID.randomUUID().toString()
        def uniqueUsers = 50
        def initialCallsPerUser = 1
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.resetCounts()
        fakeIdentityV3Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()

        and: "All users have unique X-Subject-Token"
        def userTokens = (1..uniqueUsers).collect { "random-token-$it" }

        when:
        "A burst of $uniqueUsers users sends GET requests to REPOSE with an X-Subject-Token"
        fakeIdentityV3Service.resetCounts()

        DateTime initialTokenValidation = DateTime.now()
        DateTime lastTokenValidation = DateTime.now()
        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint, method: 'GET',
                            headers: [
                                    'content-type'   : 'application/json',
                                    'X-Subject-Token': token,
                                    'TEST_THREAD'    : "User-$index-Call-$it"
                            ])
                    mc.receivedResponse.code.equals("200")
                    lastTokenValidation = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fakeIdentityV3Service.getGroupsCount == uniqueUsers

        when: "Same users send subsequent GET requests up to but not exceeding the token timeout - cache offset (since some requests may expire at that time)"
        fakeIdentityV3Service.resetCounts()
        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(GROUPS_TIMEOUT - CACHE_VARIANCE)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                while (minimumTokenExpiration.isAfterNow()) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: [
                                    'content-type'   : 'application/json',
                                    'X-Subject-Token': token])
                    mc.receivedResponse.code.equals("200")
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fakeIdentityV3Service.getGroupsCount == 0

        when: "Cache has expired for all tokens (token timeout + cache offset), and new GETs are issued"
        fakeIdentityV3Service.resetCounts()
        DateTime maximumTokenExpiration = lastTokenValidation.plusMillis(GROUPS_TIMEOUT + CACHE_VARIANCE)
        //wait until max token expiration is reached
        while (maximumTokenExpiration.isAfterNow()) {
            sleep 100
        }

        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                MessageChain mc = deproxy.makeRequest(
                        url: reposeEndpoint,
                        method: 'GET',
                        headers: [
                                'content-type'   : 'application/json',
                                'X-Subject-Token': token])
                mc.receivedResponse.code.equals("200")
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fakeIdentityV3Service.getGroupsCount == uniqueUsers
    }
}
