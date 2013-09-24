package features.filters.translation


import framework.ReposeValveTest
import framework.category.Bug
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Request
import org.rackspace.gdeproxy.Response
import framework.category.Bug
import org.junit.experimental.categories.Category;


@Category(Bug.class)
class ContentTranslationBurstTest extends ReposeValveTest {

    def static Map acceptXML = ["accept": "application/xml"]

    def static Map contentXML = ["content-type": "application/xml"]

    def static Map header1 = ["x-pp-user": "user1"]

    def static Map header2 = ["x-tenant-name": "tenant1"]

    def static String remove = "remove-me"
    def static String add = "add-me"
    def static String xmlResponse = "<a><remove-me>test</remove-me>Stuff</a>"

    //Start repose once for this particular translation test
    def setupSpec() {
        repose.applyConfigs("features/filters/translation/common",
                            "features/filters/translation/missingContent")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        def missingHeaderErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("x-pp-user") || !headers.contains("x-tenant-name")  || !headers.contains("accept") ) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING HEADERS")
            }


            return new Response(200, "OK",contentXML+header1,xmlResponse)

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
        def missingContent = false
        List<String> badRequests = new ArrayList()
        List<String> requests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-'+threadNum+'-request-'+i)
                    def resp = deproxy.makeRequest((String) reposeEndpoint, "PUT", acceptXML+header1+header2)
                    if ( resp.receivedResponse.code.equalsIgnoreCase("500")) {
                        missingHeader = true
                        badRequests.add('500-spock-thread-'+threadNum+'-request-'+i)
                        break
                    }
                    if (!resp.receivedResponse.body.contains("Stuff"))    {
                        badRequests.add('content-spock-thread-'+threadNum+'-request-'+i)
                        missingContent = true
                        break
                    }
                    if (resp.receivedResponse.headers.findAll("x-pp-user").empty)    {
                        badRequests.add('header-spock-thread-'+threadNum+'-request-'+i)
                        missingHeader = true
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

        and:
        missingContent == false


        where:
        numClients | callsPerClient
        100 | 50

    }

}
