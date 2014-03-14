package features.filters.clientauthn.cache

import framework.mocks.MockIdentityService
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
        Thread.sleep(2000)
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "should cache tokens using cache offset"() {

        given: "Identity Service returns cache tokens with 1 day expirations"
        MockIdentityService fauxIdentityService
        def clientToken = UUID.randomUUID().toString()
        fauxIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fauxIdentityService.client_token = clientToken
        fauxIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()

        and: "All users have unique X-Auth-Token"
        def userTokens = new ArrayList()
        int randomStringLength = 16
        String charset = (('a'..'z') + ('A'..'Z')).join()

        for (x in 1..uniqueUsers) {
            userTokens.add(RandomStringUtils.random(randomStringLength, charset.toCharArray()))
        }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Auth-Token"
        fauxIdentityService.resetCounts()
        Map<String,MessageChain> messageChainList = new HashMap<String,MessageChain>()

        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)

            def thread = Thread.start {
                def threadNumber = x

                for (i in 1..initialCallsPerUser) {
                    def threadName = "User_" + x + "_Call_" + i
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token, 'TEST_THREAD': threadName])
                    messageChainList.put("InitialBurst-" + x + "-" + i, mc)

                    if (threadNumber == uniqueUsers && i == 1) {
                        initialBurstLastValidationCall = DateTime.now()
                    }
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fauxIdentityService.validateTokenCount == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fauxIdentityService.resetCounts()

        Period cacheExpiration = new Period().withSeconds(20)
        DateTime minimumTokenExpiration = initialTokenValidation.plusSeconds(20)
        clientThreads = new ArrayList<Thread>()

        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)
            def Map<String, MessageChain> roundTwo = [:]

            def thread = Thread.start {
                while (DateTime.now().isBefore(minimumTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                    roundTwo.put("RoundTwo-" + x + "-" , mc)
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fauxIdentityService.validateTokenCount == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fauxIdentityService.resetCounts()
        clientThreads = new ArrayList<Thread>()

        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)
            def Map<String, MessageChain> roundTwo = [:]

            DateTime maxTokenExpiration = initialBurstLastValidationCall.plusSeconds(40)

            def thread = Thread.start {
            while (DateTime.now().isBefore(maxTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': token])
                    roundTwo.put("RoundThree-" + x + "-" , mc)
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
