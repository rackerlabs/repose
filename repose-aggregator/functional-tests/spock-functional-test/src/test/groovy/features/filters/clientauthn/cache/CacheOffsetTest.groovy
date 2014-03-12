package features.filters.clientauthn.cache

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Slow
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import org.joda.time.Period
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

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

    def "should cache tokens using cache offset"() {

        given: "Identity Service returns cache tokens with 1 day expirations"
        IdentityServiceResponseSimulator fauxIdentityService
        def clientToken = UUID.randomUUID().toString()
        fauxIdentityService = new IdentityServiceResponseSimulator()
        fauxIdentityService.client_token = clientToken
        fauxIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()

        and: "All users have unique X-Auth-Token"
        def userTokens = (1..uniqueUsers).collect { "random-token-$it" }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Auth-Token"
        fauxIdentityService.validateTokenCount = 0
        Map<String,MessageChain> messageChainList = new HashMap<String,MessageChain>()

        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        userTokens.eachWithIndex { token, index ->

            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint, method: 'GET',
                            headers: ['X-Auth-Token': token, 'TEST_THREAD': "User-$index-Call-$it"])
                    messageChainList.put("InitialBurst-$index-$it", mc)
                    mc.receivedResponse.code.equals("200")
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
        int offset = initialBurstLastValidationCall.millis - initialTokenValidation.millis

        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(offset)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def Map<String, MessageChain> roundTwo = [:]

            def thread = Thread.start {
                while (DateTime.now().isBefore(minimumTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                    roundTwo.put("RoundTwo-$index" , mc)
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

        userTokens.eachWithIndex { token, index ->
            def Map<String, MessageChain> roundTwo = [:]

            DateTime maxTokenExpiration = initialBurstLastValidationCall.plusSeconds(10)

            def thread = Thread.start {
                while (DateTime.now().isBefore(maxTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                    roundTwo.put("RoundThree-$index-" , mc)
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fauxIdentityService.validateTokenCount >= uniqueUsers - 2 // adding a little bit of wiggle for slow systems

        where:
        uniqueUsers | initialCallsPerUser
        50          | 1

    }

}
