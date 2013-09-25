package features.filters.clientauthn.burst
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response

class ValidateTokenBurstTest extends ReposeValveTest {

        def static originEndpoint
        def static identityEndpoint
        def static Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        def static Map acceptXML = ["accept": "application/xml"]


        static IdentityServiceResponseSimulator fakeIdentityService

        def setupSpec() {
            deproxy = new Deproxy()

            repose.applyConfigs("features/filters/clientauthn/common")
            repose.start()

            originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
            fakeIdentityService = new IdentityServiceResponseSimulator()
            identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                    'identity service', null, fakeIdentityService.handler)

            def missingResponseErrorHandler = { Request request ->


                if (!headers.contains("X-Auth-Token") ) {
                    return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING AUTH TOKEN")
                }


                return new Response(200, "OK",header1+acceptXML)

            }

            deproxy._defaultHandler = missingResponseErrorHandler

            Thread.sleep(10000)

        }


        def cleanupSpec() {
            if (deproxy) {
                deproxy.shutdown()
            }
            repose.stop()
        }





    def "under heavy load should not drop validate token response"() {

        given:
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

                    def resp = deproxy.makeRequest(reposeEndpoint, 'GET', header1+acceptXML)

                    if ( resp.receivedResponse.code.equalsIgnoreCase("500")) {
                        missingAuthResponse = true
                        badRequests.add('500-spock-thread-'+threadNum+'-request-'+i)
                        break
                    }
                    if (resp.receivedResponse.headers.findAll("X-Token-Expires").empty && resp.receivedResponse.headers.getFirstValue("X-Token-Expires") == expiresString)    {
                        badRequests.add('header-spock-thread-'+threadNum+'-request-'+i)
                        missingAuthHeader = true
                        break
                    }

                }
            }
            clientThreads.add(thread)
        }

        when:
        fakeIdentityService.validateTokenCount = 0
        clientThreads*.join()

        then:
        fakeIdentityService.validateTokenCount == 1

        and:
        missingAuthHeader == false

        and:
        missingAuthResponse == false

        where:
        numClients | callsPerClient
        10| 5

    }

}
