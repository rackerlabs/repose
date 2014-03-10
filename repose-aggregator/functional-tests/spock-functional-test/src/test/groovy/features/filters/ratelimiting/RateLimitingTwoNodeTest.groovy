package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Rate limiting tests ported over from python and JMeter
 */
class RateLimitingTwoNodeTest extends ReposeValveTest {
    final handler = {return new Response(200, "OK")}

    final Map<String, String> userHeaderDefault = ["X-PP-User" : "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups" : "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept" : "application/xml"]

    static int reposePort2
    static int distDatastorePort
    static int distDatastorePort2

    def getReposeEndpoint2() {
        return "http://localhost:${reposePort2}"
    }

    static int userCount = 0;
    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposePort2 = PortFinder.Singleton.getNextOpenPort()
        distDatastorePort = PortFinder.Singleton.getNextOpenPort()
        distDatastorePort2 = PortFinder.Singleton.getNextOpenPort()

        def params = properties.getDefaultTemplateParams()
        params += [
                reposePort2: reposePort2,
                distDatastorePort: distDatastorePort,
                distDatastorePort2: distDatastorePort2
        ]


        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "When Repose is configured with multiple nodes, rate-limiting info should be shared"() {
        given: "load the configs for multiple nodes, and use all remaining requests"
        useAllRemainingRequests()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint2, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited, and does not pass to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }


    def "when a burst of limits is sent for an execution, only 2x-1 requests can get through"() {

        given:
        def user = getNewUniqueUser()
        def group = "customer"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        List<Thread> clientThreads = new ArrayList<Thread>()
        def totalSuccessfulCount = 0
        def totalFailedCount = 0
        List<String> requests = new ArrayList()
        int rate_limiting_count = 5

        long start = System.currentTimeMillis()

        numClients.times { x ->

            def thread = Thread.start {
                def threadNum = x

                callsPerClient.times { i ->
                    requests.add("spock-thread-$threadNum-request-$i")
                    def messageChain = null
                    if((i+x) % 2 == 0){
                        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)
                        println reposeEndpoint
                    }
                    else{
                        messageChain = deproxy.makeRequest(url: reposeEndpoint2, method: "GET", headers: headers)
                        println reposeEndpoint2
                    }
                    println messageChain.receivedResponse.code
                    if(messageChain.receivedResponse.code.equals("200")){
                        totalSuccessfulCount = totalSuccessfulCount + 1
                    }
                    else   {
                        totalFailedCount = totalFailedCount + 1
                    }

                    Thread.sleep(1000)

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        long stop = System.currentTimeMillis()
        assert totalSuccessfulCount <= (rate_limiting_count * 2 - 1)
        println totalSuccessfulCount
        println rate_limiting_count

        where:
        numClients | callsPerClient
        10         | 50
        50         | 10
    }

    private int parseRemainingFromXML(String s, int limit) {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        Document document = documentBuilder.parse(new InputSource(new StringReader(s)))

        document.getDocumentElement().normalize()

        return Integer.parseInt(document.getElementsByTagName("limit").item(limit).getAttributes().getNamedItem("remaining").getNodeValue())
    }

    private int parseAbsoluteFromXML(String s, int limit) {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        Document document = documentBuilder.parse(new InputSource(new StringReader(s)))

        document.getDocumentElement().normalize()

        return Integer.parseInt(document.getElementsByTagName("limit").item(limit).getAttributes().getNamedItem("value").getNodeValue())
    }

    private String getLimits() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + groupHeaderDefault + acceptHeaderDefault);

        return messageChain.receivedResponse.body
    }

    private void waitForLimitReset() {
        while (parseRemainingFromXML(getLimits(), 1) != 5) {
            sleep(10000)
        }
    }

    private void useAllRemainingRequests() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);

        while(!messageChain.receivedResponse.code.equals("413")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                    headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);
        }
    }
}
