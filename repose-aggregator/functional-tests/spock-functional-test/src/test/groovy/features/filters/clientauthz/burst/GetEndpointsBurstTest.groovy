package features.filters.clientauthz.burst

import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class GetEndpointsBurstTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        repose.configurationProvider.applyConfigs("common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/common", properties.defaultTemplateParams)
        repose.start()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        Map acceptXML = ["accept": "application/xml"]

        def missingResponseErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-Auth-Token")) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING AUTH TOKEN")
            }
            return new Response(200, "OK", header1 + acceptXML)
        }
        deproxy.defaultHandler = missingResponseErrorHandler
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll("Testing with #numClients clients for #callsPerClient clients")
    def "under heavy load should not drop get endpoints response"() {

        given:
        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        fakeIdentityService.resetCounts()

        List<Thread> clientThreads = new ArrayList<Thread>()

        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.US)
                .withZone(DateTimeZone.UTC);
        def expiresString = fmt.print(fakeIdentityService.tokenExpiresAt);

        def missingAuthResponse = false
        def Bad403Response = false
        List<String> badRequests = new ArrayList()
        List<String> requests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)

                    def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: header1)

                    if (messageChain.receivedResponse.code.equalsIgnoreCase("500")) {
                        missingAuthResponse = true
                        badRequests.add('500-spock-thread-' + threadNum + '-request-' + i)
                    }

                    if (messageChain.receivedResponse.code.equalsIgnoreCase("403")) {
                        Bad403Response = true
                        badRequests.add('403-spock-thread-' + threadNum + '-request-' + i)
                    }

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        fakeIdentityService.getEndpointsCount == 1

        and:
        Bad403Response == false

        and:
        missingAuthResponse == false

        where:
        numClients | callsPerClient
        10         | 5
        20         | 10
        50         | 10
        100        | 5
    }

}
