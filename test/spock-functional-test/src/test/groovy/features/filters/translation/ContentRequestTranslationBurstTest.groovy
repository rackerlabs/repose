package features.filters.translation
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class ContentRequestTranslationBurstTest extends ReposeValveTest {

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
                "features/filters/translation/missingContent/request")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())


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
        def badContent = false
        def missingContent = false
        List<String> badRequests = new ArrayList()
        List<String> requests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x
                System.out.println("Thread: ${threadNum} starting...")
                for (i in 1..callsPerClient) {
                    //System.out.println("Thread: ${threadNum} Request: ${i}")

                    requests.add('spock-thread-'+threadNum+'-request-'+i)
                    def resp = deproxy.makeRequest(url:(String) reposeEndpoint, method:"POST", headers: acceptXML+header1+header2+contentXML,requestBody: xmlResponse)

                    //assert resp.receivedResponse.code == "200"
                    if(resp.handlings.size()>0) {
                        if (!resp.handlings.get(0).request.body.toString().contains("Stuff"))
                        {
                            badRequests.add('content-spock-thread-'+threadNum+'-request-'+i)
                            missingContent = true
                            break
                        }
                        if (resp.handlings.get(0).request.body.toString().contains("remove-me"))    {
                            badRequests.add('content-spock-thread-'+threadNum+'-request-'+i)
                            badContent = true
                            break
                        }
                        if (resp.handlings.get(0).request.headers.findAll("x-pp-user").empty)    {
                            badRequests.add('header-spock-thread-'+threadNum+'-request-'+i)
                            missingHeader = true
                            break
                        }

                    }
                    /**if ( resp.receivedResponse.code.equalsIgnoreCase("500")) {
                     missingHeader = true
                     badRequests.add('500-spock-thread-'+threadNum+'-request-'+i)
                     break
                     }
                     if (!resp.receivedResponse.body.contains("Stuff"))    {
                     badRequests.add('content-spock-thread-'+threadNum+'-request-'+i)
                     missingContent = true
                     break
                     }
                     if (resp.receivedResponse.body.contains("remove-me"))    {
                     badRequests.add('content-spock-thread-'+threadNum+'-request-'+i)
                     badContent = true
                     break
                     }

                     if (resp.receivedResponse.headers.findAll("x-pp-user").empty)    {
                     badRequests.add('header-spock-thread-'+threadNum+'-request-'+i)
                     missingHeader = true
                     break
                     } **/

                }
                //System.out.println("Thread: ${threadNum} finished.")

            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        missingHeader == false

        and:
        missingContent == false

        and:
        badContent == false


        where:
        numClients | callsPerClient
        100| 500

    }

}
