package features.filters.translation

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy


@Category(Slow.class)
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
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/translation/common",
                "features/filters/translation/missingContent/request")
        repose.start()
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
        def noHandlings = false
        List<String> requests = new ArrayList()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)
                    def messageChain = deproxy.makeRequest(url: (String) reposeEndpoint, method: "POST", headers: acceptXML + header1 + header2 + contentXML, requestBody: xmlResponse)

                    if (messageChain.handlings.size() == 0) {
                        noHandlings = true
                        break
                    }
                    if (!messageChain.handlings.get(0).request.body.toString().equals("<a>Stuff</a>")) {
                        missingContent = true
                        break
                    }
                    if (messageChain.handlings.get(0).request.body.toString().contains("remove-me")) {
                        badContent = true
                        break
                    }
                    if (messageChain.handlings.get(0).request.headers.findAll("x-pp-user").empty) {
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
        missingContent == false
        badContent == false
        noHandlings == false

        where:
        numClients | callsPerClient
        100        | 50

    }

}
