package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by jennyvo on 8/27/14.
 * Test token cached with no cache-offset attr or set to 0
 */
class KeystoneV3NoCacheOffSetTest extends ReposeValveTest{
    @Shared def identityEndpoint
    @Shared def MockKeystoneV3Service fakeKeystoneV3Service

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop([throwExceptionOnKill: false])

    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of tokenTimeout +/- cacheOffset
     * - all tokens will expire at tokenTimeout+cacheOffset
     */
    @Unroll("when cache-offset is not config or equals 0 and token-cache-timeout #tokenTimeout: #id")
    def "when cache offset is not config or set to 0 no cache offset is used"() {

        given: "All users have unique X-Auth-Token"
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/cacheoffset/"+additionalConfigs, params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort)
        fakeKeystoneV3Service.resetCounts()
        fakeKeystoneV3Service.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)

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
        fakeKeystoneV3Service.validateTokenCount == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fakeKeystoneV3Service.resetCounts()

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
        fakeKeystoneV3Service.validateTokenCount == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fakeKeystoneV3Service.resetCounts()
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
        fakeKeystoneV3Service.validateTokenCount == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | additionalConfigs   | id   | tokenTimeout
        10          | 4                   | "notset"            | 100  | 5000
        15          | 4                   | "defaultzero"       | 200  | 5000
    }
}
