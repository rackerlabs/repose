import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders

/**
 * Created by jennyvo on 9/17/14.
 * Test basic auth cache acts the same as client-auth
 */
@Ignore("Ignore this test for now - will revisit and update when this caching functionality implemented")
class BasicAuthCacheTest extends ReposeValveTest {

    @Shared def identityEndpoint
    @Shared def MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
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
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        fakeIdentityService.resetCounts()
        fakeIdentityService.with {
            client_apikey = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1)
        }

        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        List<Thread> clientThreads = new ArrayList<Thread>()
        def userTokens = (1..uniqueUsers).collect { "another-cache-offset-random-token-$id-$it" }
        def authorized = Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)


        when: "A burst of XXX users sends GET requests to REPOSE with username/apikey"

        DateTime initialTokenValidation = DateTime.now()
        DateTime initialBurstLastValidationCall
        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                (1..initialCallsPerUser).each {
                    def threadName = "User-$index-Call-$it"
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: [
                                    (HttpHeaders.AUTHORIZATION): 'Basic ' + authorized,
                                    'TEST_THREAD': threadName])
                    mc.receivedResponse.code.equals('200')

                    initialBurstLastValidationCall = DateTime.now()
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "REPOSE should validate the token and then pass the request to the origin service"
        fakeIdentityService.getValidateTokenCount() == uniqueUsers


        when: "Same users send subsequent GET requests up to but not exceeding the cache expiration"
        fakeIdentityService.resetCounts()

        DateTime minimumTokenExpiration = initialTokenValidation.plusMillis(tokenTimeout)
        clientThreads = new ArrayList<Thread>()

        userTokens.eachWithIndex { token, index ->
            def thread = Thread.start {
                while (minimumTokenExpiration.isAfterNow()) {
                    MessageChain mc = deproxy.makeRequest(
                            url: reposeEndpoint,
                            method: 'GET',
                            headers: [(HttpHeaders.AUTHORIZATION): 'Basic ' + authorized])
                    mc.receivedResponse.code.equals('200')
                }
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit cache"
        fakeIdentityService.getValidateTokenCount() == 0

        when: "Cache has expired for all tokens, and new GETs are issued"
        fakeIdentityService.resetCounts()
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
                        headers: [(HttpHeaders.AUTHORIZATION): 'Basic ' + authorized])
                mc.receivedResponse.code.equals('200')
            }
            clientThreads.add(thread)
        }
        clientThreads*.join()

        then: "All calls should hit identity"
        fakeIdentityService.getValidateTokenCount() == uniqueUsers

        where:
        uniqueUsers | initialCallsPerUser | id | tokenTimeout
        10          | 4                   | 1  | 5000
        10          | 4                   | 2  | 5000

    }
}