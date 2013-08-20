package features.filters.headertranslation

import framework.ReposeValveTest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response


class ReliabilityTest extends ReposeValveTest {

    //Start repose once for this particular translation test
    def setupSpec() {
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/oneToMany" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        def missingHeaderErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-Header-C") || !headers.contains("X-Header-D")) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING HEADERS")
            }

            return new Response(200, "OK")
        }

        deproxy._defaultHandler = missingHeaderErrorHandler

        Thread.sleep(10000)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "under heavy load should not drop headers"() {

        given:
        List<Thread> clientThreads = new ArrayList<Thread>()

        def missingHeader = false
        List<String> badRequests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    def HttpClient client = new DefaultHttpClient()

                    HttpGet httpGet = new HttpGet(reposeEndpoint)
                    httpGet.addHeader('X-Header-A','lisa.rocks')
                    httpGet.addHeader('thread-name', 'spock-thread-'+threadNum+'-request-'+i)

                    HttpResponse response = client.execute(httpGet)
                    if (response.getStatusLine().getStatusCode() == 500) {
                        missingHeader = true
                        badRequests.add('spock-thread-'+threadNum+'-request-'+i)
                        break
                    }
                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        missingHeader == false

        where:
        numClients | callsPerClient
        200 | 100
    }

}
