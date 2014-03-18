package features.filters.clientauthn.cache

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Slow
import framework.mocks.MockIdentityService
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import org.joda.time.Period
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import java.util.concurrent.atomic.AtomicInteger

@Category(Slow.class)
class CacheOffsetTest extends ReposeValveTest {

    def identityEndpoint

    //Start repose once for this particular translation test
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        repose.configurationProvider.applyConfigs("common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/cacheoffset", properties.defaultTemplateParams)
        repose.start()
        waitUntilReadyToServiceRequests('401')
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    /**
     * Cache offset test will test the following scenario:
     * - a burst of requests will be sent for a specified number of users
     * - cache timeout for these users will be set at a range of tokenTimeout +/- cacheOffset
     * - all tokens will expire at tokenTimeout+cacheOffset
     */
    def "should cache tokens using cache offset"() {

        given: "Identity Service returns cache tokens with 1 day expirations"
        MockIdentityService fauxIdentityService
        def (clientToken,tokenTimeout,cacheOffset) = [UUID.randomUUID().toString(),5000,3000]
        fauxIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fauxIdentityService.resetCounts()
        fauxIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()

        and: "All users have unique X-Auth-Token"
        def userTokens = (1..uniqueUsers).collect { "random-token-$it" }

        when: "A burst of $uniqueUsers users sends GET requests to REPOSE with an X-Auth-Token"
        fauxIdentityService.resetCounts()

        DateTime initialTokenValidation = DateTime.now()
        DateTime lastTokenValidation = DateTime.now()
        userTokens.eachWithIndex { token, index ->

            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint, method: 'GET',
                            headers: ['X-Auth-Token': token, 'TEST_THREAD': "User-$index-Call-$it"])
                    mc.receivedResponse.code.equals("200")
                    lastTokenValidation = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fauxIdentityService.validateTokenCount.get() == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the token timeout - cache offset (since some requests may expire at that time)"
        fauxIdentityService.resetCounts()

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
        fauxIdentityService.validateTokenCount.get() == 0

        when: "Cache has expired for all tokens (token timeout + cache offset), and new GETs are issued"
        fauxIdentityService.resetCounts()
        DateTime maximumTokenExpiration = lastTokenValidation.plusMillis(tokenTimeout + cacheOffset)
        //wait until max token expiration is reached
        while (maximumTokenExpiration.isAfterNow()) {
            sleep 100
        }

        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                mc.receivedResponse.code.equals("200")
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        //since we are talking about time based testing, we cannot always validate against a concrete number.  This is testing a range of requests.
        fauxIdentityService.validateTokenCount.get() == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser
        50          | 1

    }

}
