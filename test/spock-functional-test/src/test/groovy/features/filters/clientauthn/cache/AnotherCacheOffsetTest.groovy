package features.filters.clientauthn.cache
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Flaky
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Shared

@Category(Flaky)
class AnotherCacheOffsetTest extends ReposeValveTest {

    @Shared def identityEndpoint
    @Shared def IdentityServiceResponseSimulator fauxIdentityService

    def cleanup() {
        deproxy.shutdown()
        repose.stop()
    }

    def "when cache offset is not configured then no cache offset is used"() {

        given: "All users have unique X-Auth-Token"
        repose.applyConfigs("features/filters/clientauthn/cacheoffset/common",
                additionalConfigs)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        Thread.sleep(2000)

        def clientToken = UUID.randomUUID().toString()
        fauxIdentityService = new IdentityServiceResponseSimulator()
        fauxIdentityService.client_token = clientToken
        fauxIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fauxIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
        def userTokens = new ArrayList()
        int randomStringLength = 16
        String charset = (('a'..'z') + ('A'..'Z')).join()

        for (x in 1..uniqueUsers) {
            userTokens.add(RandomStringUtils.random(randomStringLength, charset.toCharArray()))
        }

        when: "A burst of XXX users sends GET requests to REPOSE with an X-Auth-Token"
        fauxIdentityService.validateTokenCount = 0
        Map<String,MessageChain> messageChainList = new HashMap<String,MessageChain>()

        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)

            def thread = Thread.start {
                def threadNumber = x

                for (i in 1..initialCallsPerUser) {
                    def threadName = "User_" + x + "_Call_" + i
                    MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': token, 'TEST_THREAD': threadName])
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
        fauxIdentityService.validateTokenCount = 0

        DateTime minimumTokenExpiration = initialTokenValidation.plusSeconds(30)
        clientThreads = new ArrayList<Thread>()

        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)
            def Map<String, MessageChain> roundTwo = [:]

            def thread = Thread.start {
                while (DateTime.now().isBefore(minimumTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': token])
                    roundTwo.put("RoundTwo-" + x + "-" , mc)
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

        for (int x in 1..uniqueUsers) {
            def token = userTokens.get(x-1)
            def Map<String, MessageChain> roundTwo = [:]

            DateTime maxTokenExpiration = initialBurstLastValidationCall.plusSeconds(30)

            def thread = Thread.start {
            while (DateTime.now().isBefore(maxTokenExpiration)) {
                    MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': token])
                    roundTwo.put("RoundThree-" + x + "-" , mc)
            }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fauxIdentityService.validateTokenCount == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser |additionalConfigs
        10          | 4                   | "features/filters/clientauthn/cacheoffset/notset"
        10          | 4                   | "features/filters/clientauthn/cacheoffset/defaultzero"

    }

}
