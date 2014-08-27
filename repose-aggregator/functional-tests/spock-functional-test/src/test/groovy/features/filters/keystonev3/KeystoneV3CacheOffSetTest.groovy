package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by jennyvo on 8/26/14.
 * Test with keystone v3 with cache-offset option
 */
class KeystoneV3CacheOffSetTest extends ReposeValveTest{
    def identityEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/common",params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/cacheoffset",params)
        repose.start()
        waitUntilReadyToServiceRequests("401")
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of tokenTimeout +/- cacheOffset
     * - all tokens will expire at tokenTimeout+cacheOffset
     */
    def "should cache tokens using cache offset"() {
        given: "Identity Service returns cache tokens with 1 day expiration"
        MockKeystoneV3Service fakeKeystoneV3Service
        def (clientToken,tokenTimeout,cacheOffset) = [UUID.randomUUID().toString(),5000,3000]
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort, properties.targetPort)
        fakeKeystoneV3Service.resetCounts()
        fakeKeystoneV3Service.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null,fakeKeystoneV3Service.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()

        and: "All users have unique X-Auth-Token"
        def userTokens = (1..uniqueUsers).collect { "random-token-$it" }

        when: "A burst of $uniqueUsers users sends GET requests to REPOSE with an X-Auth-Token"
        fakeKeystoneV3Service.resetCounts()

        DateTime initialTokenValidation = DateTime.now()
        DateTime lastTokenValidation = DateTime.now()
        userTokens.eachWithIndex { token, index ->

            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint, method: 'GET',
                            headers: ['X-Subject-Token': token, 'TEST_THREAD': "User-$index-Call-$it"])
                    mc.receivedResponse.code.equals("200")
                    lastTokenValidation = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fakeKeystoneV3Service.validateTokenCount == uniqueUsers

        when: "Same users send subsequent GET requests up to but not exceeding the token timeout - cache offset (since some requests may expire at that time)"
        fakeKeystoneV3Service.resetCounts()
        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(tokenTimeout - cacheOffset)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                while (minimumTokenExpiration.isAfterNow()) {
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                    mc.receivedResponse.code.equals("200")
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fakeKeystoneV3Service.validateTokenCount == 0

        when: "Cache has expired for all tokens (token timeout + cache offset), and new GETs are issued"
        fakeKeystoneV3Service.resetCounts()
        DateTime maximumTokenExpiration = lastTokenValidation.plusMillis(tokenTimeout + cacheOffset)
        //wait until max token expiration is reached
        while (maximumTokenExpiration.isAfterNow()) {
            sleep 100
        }

        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Subject-Token': token])
                mc.receivedResponse.code.equals("200")
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        //since we are talking about time based testing, we cannot always validate against a concrete number.  This is testing a range of requests.
        fakeKeystoneV3Service.validateTokenCount == uniqueUsers

        where:
        [uniqueUsers, initialCallsPerUser] << [50, 1]
    }
}
