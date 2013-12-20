package features.filters.headertranslation
import framework.ReposeValveTest
import framework.category.Bug
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

@org.junit.experimental.categories.Category(Bug.class)
class ReliabilityTest extends ReposeValveTest {

    //Start repose once for this particular translation test
    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/headertranslation/common", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def missingHeaderErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-OneToMany-C") || !headers.contains("X-OneToMany-D")) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING HEADERS")
            }

            return new Response(200, "OK")
        }

        deproxy.defaultHandler = missingHeaderErrorHandler

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
                    httpGet.addHeader('X-OneToMany-A','lisa.rocks')
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
