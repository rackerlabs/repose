package features.filters.clientauthn.ae_token
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
/**
 * Created by jennyvo on 1/16/15.
 * To determine Repose readiness to support AE Token
 * Test check if repose can handle token up to 250 bytes and a large number of tokens in cache
 *
 */
class AETokenSupportTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    String charset = (('A'..'Z') + ('0'..'9')).join()

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup(){
        fakeIdentityService.resetHandlers()
    }

    def "Support heavy load and large string token" () {
        given:
        fakeIdentityService.resetCounts()
        List<Thread> clientThreads = new ArrayList<Thread>()

        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.US)
                .withZone(DateTimeZone.UTC);

        def missingAuthResponse = false
        def missingAuthHeader = false

        (1..numClients).each {
            threadNum ->
                //Map header1 = ['X-Auth-Token': RandomStringUtils.random(250, charset)]
                def thread = Thread.start {
                    (1..callsPerClient).each {
                        Map header1 = ['X-Auth-Token': RandomStringUtils.random(250, charset)]
                        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: header1)

                        if (messageChain.receivedResponse.code.equalsIgnoreCase("500")) {
                            println messageChain.receivedResponse.body
                            if (messageChain.orphanedHandlings.size() > 0) {
                                println messageChain.orphanedHandlings[0].request.body
                                println messageChain.orphanedHandlings[0].response.body
                            }
                            missingAuthResponse = true
                        } else {
                            def sentToOrigin = ((MessageChain) messageChain).getHandlings()[0]
                            if (sentToOrigin.request.headers.findAll("x-roles").empty) {
                                println sentToOrigin.request.headers
                                missingAuthHeader = true
                            }
                        }
                    }
                }
                clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        fakeIdentityService.generateTokenCount == 1
        fakeIdentityService.validateTokenCount == numClients * callsPerClient

        and:
        missingAuthHeader == false

        and:
        missingAuthResponse == false

        where:
        numClients | callsPerClient
        50         | 150
    }
}
