package features.filters.clientauthn.cache
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Flaky
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared
import spock.lang.Unroll

@Category(Flaky)
class AnotherCacheOffsetTest extends ReposeValveTest {

    @Shared def identityEndpoint
    @Shared def IdentityServiceResponseSimulator fauxIdentityService

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
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/cacheoffset/common", params)
        repose.configurationProvider.applyConfigs(additionalConfigs, params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        def clientToken = UUID.randomUUID().toString()
        fauxIdentityService = new IdentityServiceResponseSimulator()
        fauxIdentityService.client_token = clientToken
        fauxIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
        def userTokens = (1..uniqueUsers).collect { "another-cache-offset-random-token-$id-$it" }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Auth-Token"
        fauxIdentityService.validateTokenCount = 0

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
        fauxIdentityService.validateTokenCount == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fauxIdentityService.validateTokenCount = 0

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
        fauxIdentityService.validateTokenCount == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fauxIdentityService.validateTokenCount = 0
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
        fauxIdentityService.validateTokenCount == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | additionalConfigs                                      | id | tokenTimeout | cacheOffset
        10          | 4                   | "features/filters/clientauthn/cacheoffset/notset"      | 1  | 5000         | 0
        10          | 4                   | "features/filters/clientauthn/cacheoffset/defaultzero" | 2  | 5000         | 0

    }

}
