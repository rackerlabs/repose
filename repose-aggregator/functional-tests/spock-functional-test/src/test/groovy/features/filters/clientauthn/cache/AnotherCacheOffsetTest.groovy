package features.filters.clientauthn.cache

import framework.mocks.MockIdentityService
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
    @Shared def MockIdentityService fauxIdentityService

    def cleanup() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("when cache offset is not configured then no cache offset is used - #id")
    def "when cache offset is not configured then no cache offset is used"() {

        given: "All users have unique X-Auth-Token"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/cacheoffset/common", params)
        repose.configurationProvider.applyConfigs(additionalConfigs, params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        Thread.sleep(2000)

        def clientToken = UUID.randomUUID().toString()
        fauxIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fauxIdentityService.client_token = clientToken
        fauxIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
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
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Auth-Token': token, 'TEST_THREAD': threadName])
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

        DateTime minimumTokenExpiration = initialTokenValidation.plusSeconds(30)
        clientThreads = new ArrayList<Thread>()

        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)
            def Map<String, MessageChain> roundTwo = [:]

            def thread = Thread.start {
                while (DateTime.now().isBefore(minimumTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Auth-Token': token])
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

            DateTime maxTokenExpiration = initialBurstLastValidationCall.plusSeconds(30)

            def thread = Thread.start {
            while (DateTime.now().isBefore(maxTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: ['X-Auth-Token': token])
                    roundTwo.put("RoundThree-" + x + "-" , mc)
            }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fauxIdentityService.validateTokenCount == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | additionalConfigs                                      | id
        10          | 4                   | "features/filters/clientauthn/cacheoffset/notset"      | 1
        10          | 4                   | "features/filters/clientauthn/cacheoffset/defaultzero" | 2

    }

}
