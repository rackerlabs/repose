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
package features.filters.keystonev2.cache

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared
import spock.lang.Unroll

class AnotherCacheOffsetTest extends ReposeValveTest {

    @Shared
    def identityEndpoint
    @Shared
    def MockIdentityService fauxIdentityService

    def cleanup() {
        deproxy.shutdown()
        repose.stop()
    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of tokenTimeout +/- cacheOffset
     * - all tokens will expire at tokenTimeout+cacheOffset
     */
    @Unroll("when cache offset is not configured then no cache offset is used - #id")
    def "when cache offset is not configured then no cache offset is used"() {

        given: "All users have unique X-Auth-Token"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev2/cacheoffset/common", params)
        repose.configurationProvider.applyConfigs(additionalConfigs, params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        fauxIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fauxIdentityService.resetCounts()
        fauxIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
        def userTokens = (1..uniqueUsers).collect { "another-cache-offset-random-token-$id-$it" }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Auth-Token"

        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    def threadName = "User-$index-Call-$it"
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Auth-Token': token, 'TEST_THREAD': threadName])
                    mc.receivedResponse.code.equals('200')

                    initialBurstLastValidationCall = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fauxIdentityService.getValidateTokenCount() == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fauxIdentityService.resetCounts()

        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(tokenTimeout - cacheOffset)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                while (minimumTokenExpiration.isAfterNow()) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Auth-Token': token])
                    mc.receivedResponse.code.equals('200')
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fauxIdentityService.getValidateTokenCount() == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fauxIdentityService.resetCounts()
        clientThreads = new ArrayList<Thread>()

        DateTime maxTokenExpiration = initialBurstLastValidationCall.plusMillis(tokenTimeout + cacheOffset)
        while (maxTokenExpiration.isAfterNow()) {
            sleep 500
        }

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                MessageChain mc = deproxy.makeRequest(
                        url: reposeEndpoint,
                        method: 'GET',
                        headers: ['X-Auth-Token': token])
                mc.receivedResponse.code.equals('200')
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fauxIdentityService.getValidateTokenCount() == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | additionalConfigs                                      | id | tokenTimeout | cacheOffset
        10          | 4                   | "features/filters/clientauthn/cacheoffset/notset"      | 1  | 5000         | 0
        10          | 4                   | "features/filters/clientauthn/cacheoffset/defaultzero" | 2  | 5000         | 0

    }

}
