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
import spock.lang.Shared
import spock.lang.Unroll

@Category(Filters)
class IdentityV3NoCacheOffSetTest extends ReposeValveTest {
    @Shared def identityEndpoint
    @Shared MockIdentityV3Service fakeIdentityV3Service

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop(throwExceptionOnKill: false)
    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of tokenTimeout +/- cacheOffset
     * - all tokens will expire at tokenTimeout+cacheOffset
     */
    @Unroll
    def "when cache offset is not config or equals 0, token-cache-timeout of #tokenTimeout, and ID #id, then no cache offset is used"() {

        given: "All users have unique X-Auth-Token"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/cacheoffset/" + additionalConfigs, params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        fakeIdentityV3Service.resetCounts()
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
        def userTokens = (1..uniqueUsers).collect { "random-token-$id-$it" }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Subject-Token"
        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    def threadName = "User-$index-Call-$it"
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Subject-Token': token, 'TEST_THREAD': threadName])
                    assert mc.receivedResponse.code.equals('200')

                    initialBurstLastValidationCall = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fakeIdentityV3Service.validateTokenCount == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fakeIdentityV3Service.resetCounts()

        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(tokenTimeout)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                while (minimumTokenExpiration.isAfterNow()) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Subject-Token': token])
                    assert mc.receivedResponse.code.equals('200')
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fakeIdentityV3Service.validateTokenCount == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fakeIdentityV3Service.resetCounts()
        clientThreads = new ArrayList<Thread>()

        DateTime maxTokenExpiration = initialBurstLastValidationCall.plusMillis(tokenTimeout)
        while (maxTokenExpiration.isAfterNow()) {
            sleep 500
        }

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                MessageChain mc = deproxy.makeRequest(
                        url: reposeEndpoint,
                        method: 'GET',
                        headers: ['X-Subject-Token': token])
                assert mc.receivedResponse.code.equals('200')
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fakeIdentityV3Service.validateTokenCount == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | additionalConfigs | id  | tokenTimeout
        10          | 4                   | "notset"          | 100 | 5000
        15          | 4                   | "defaultzero"     | 200 | 5000
    }
}
