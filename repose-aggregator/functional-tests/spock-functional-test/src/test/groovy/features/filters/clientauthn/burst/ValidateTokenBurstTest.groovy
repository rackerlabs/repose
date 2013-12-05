package features.filters.clientauthn.burst
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

class ValidateTokenBurstTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    static IdentityServiceResponseSimulator fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/common")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new IdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        Map acceptXML = ["accept": "application/xml"]

        def missingResponseErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-Auth-Token") ) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING AUTH TOKEN")
            }


            return new Response(200, "OK",header1+acceptXML)

        }

        deproxy.defaultHandler = missingResponseErrorHandler
    }


    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    def "under heavy load should not drop validate token response"() {

        given:
        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        fakeIdentityService.validateTokenCount = 0

        List<Thread> clientThreads = new ArrayList<Thread>()

        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.US)
                .withZone(DateTimeZone.UTC);
        def expiresString = fmt.print(fakeIdentityService.tokenExpiresAt);

        def missingAuthResponse = false
        def missingAuthHeader = false
        List<String> badRequests = new ArrayList()
        List<String> requests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-'+threadNum+'-request-'+i)

                    def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: header1)

                    if (messageChain.receivedResponse.code.equalsIgnoreCase("500")) {
                        missingAuthResponse = true
                        badRequests.add('500-spock-thread-'+threadNum+'-request-'+i)
                    }


                    def sentToOrigin = ((MessageChain) messageChain).getHandlings()[0]

                    if (sentToOrigin.request.headers.findAll("x-roles").empty) {
                        badRequests.add('header-spock-thread-'+threadNum+'-request-'+i)
                        missingAuthHeader = true
                    }

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        fakeIdentityService.validateTokenCount == 1

        and:
        fakeIdentityService.groupsCount == 1

        and:
        missingAuthHeader == false

        and:
        missingAuthResponse == false

        where:
        numClients | callsPerClient
        10 | 5

    }

}
