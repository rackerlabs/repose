package features.filters.ratelimiting

import framework.ReposeValveTest
import groovy.json.JsonSlurper
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by jennyvo on 7/30/14.
 */
class GlobalRateLimitingTest extends ReposeValveTest {
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

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/onenodes", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    /*
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
        //repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalratelimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }
    */
    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def cleanup() {
        waitForLimitReset()
    }

    def "When Repose config with Global Rate Limit, user limit should hit first" () {
        given: "the rate-limit has not been reached"
        waitForLimitReset()

        (1..5).each{
            i ->
                when: "the user sends their request and the rate-limit has not been reached"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                messageChain.receivedResponse.code.equals("200")
                messageChain.handlings.size() == 1
        }

        when: "the user hit the rate-limit"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("413")
    }

    /*
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
    */
    private void useAllRemainingRequests() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);

        while(!messageChain.receivedResponse.code.equals("413")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                    headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);
        }
    }

    private void waitForLimitReset(Map group = null) {
        while (parseRemainingFromXML(getDefaultLimits(group), 0) != parseAbsoluteFromXML(getDefaultLimits(group), 0)) {
            sleep(1000)
        }
    }

    private String getDefaultLimits(Map group = null) {
        def groupHeader = (group != null) ? group : groupHeaderDefault
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + groupHeader + acceptHeaderDefault);

        return messageChain.receivedResponse.body
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
}
