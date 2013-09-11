package features.filters.ratelimiting

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Rate limiting tests ported over from python and JMeter
 */
@Category(Slow.class)
class RateLimitingTest extends ReposeValveTest {
    final handler = {return new Response(200, "OK")}

    final Map<String, String> userHeaderDefault = ["X-PP-User" : "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups" : "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept" : "application/xml"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/ratelimiting/onenodes/")
        repose.start()

        sleep(5000)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "When a limit is tested, method should not make a difference"() {
        given: "the rate-limit has not been reached"
        waitForLimitReset()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "POST",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "PUT",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "DELETE",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "HEAD",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "When a limit has not been reached, request should pass"() {
        given: "the rate-limit has not been reached"
        // A new user is used to prevent waiting for rate limit to reset

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["X-PP-User" : "tester"] + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        where:
        i << [0..4]
    }

    def "When a limit has been reached, request should not pass"() {
        given: "the rate-limit has been reached"
        useAllRemainingRequests()

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "When a limit has been reached, the limit should reset after one minute"() {
        given: "the limit has been reached"
        useAllRemainingRequests()

        when: "another request is sent"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0

        when: "a minute passes, and another request is sent"
        sleep(60000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "rate limit should have reset, and request should succeed"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
    }

    def "When rate limiting requests with multiple X-PP-User values, should allow requests with new username"() {
        given: "the limit has been reached for the default user"
        useAllRemainingRequests()

        when: "a request is made by a different user"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["X-PP-User" : "other_user;q=1.0"] + groupHeaderDefault, defaultHandler: handler)

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting requests with multiple X-PP-Group values, should allow requests with new group with higher priority"() {
        given: "the limit has been reached for a user in a certain group"
        useAllRemainingRequests()

        when: "a request is made using a new group with a higher quality"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups" : "customer;q=0.5,higher;q=0.75"])

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")
    }

    def "When requesting rate limits, should receive rate limits in request format"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups" : "unlimited;q=1.0", "X-PP-User" : "user"] + acceptHeader)

        then:
        messageChain.receivedResponse.code.equals("200")
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)
        messageChain.receivedResponse.body.length() > 0

        // TODO why aren't the .formats working?
        where:
        path                   | acceptHeader                       | expectedFormat
        "/service/limits"      | ["Accept" : "application/xml"]     | "application/xml"
        //"/service/limits.xml"  | ["Accept" : "application/xml"]     | "application/xml"
        //"/service/limits.xml"  | ["Accept" : "application/json"]    | "application/xml"
        //"/service/limits.xml"  | ["Accept" : "application/unknown"] | "application/xml"
        //"/service/limits.xml"  | ["Accept" : ""]                    | "application/xml"
        "/service/limits"      | ["Accept" : "application/json"]    | "application/json"
        //"/service/limits.json" | ["Accept" : "application/json"]    | "application/json"
        //"/service/limits.json" | ["Accept" : "application/xml"]     | "application/json"
        //"/service/limits.json" | ["Accept" : "application/unknown"] | "application/json"
        //"/service/limits.json" | ["Accept" : ""]                    | "application/json"
    }

    def "When requesting rate limits with invalid Accept header, Should receive 406 response when invalid Accept header"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-Groups" : "unlimited;q=1.0", "X-PP-User" : "user", "Accept" : "invalid/test"])

        then:
        messageChain.receivedResponse.code.equals("406")
    }

    def "When rate limiting against multiple regexes, Should not limit requests against a different regex"() {
        given:
        useAllRemainingRequests("user", "multiregex", "/endpoint1")

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/endpoint1", method: "GET",
                headers: ["X-PP-Groups" : "multiregex", "X-PP-User" : "user"])

        then:
        messageChain.receivedResponse.code.equals("413")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/endpoint2", method: "GET",
                headers: ["X-PP-Groups" : "multiregex", "X-PP-User" : "user"])

        then:
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting against ALL HTTP methods, should"() {
        given:
        useAllRemainingRequests("user", "all-limit", "")

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "POST",
                headers: ["X-PP-Groups" : "all-limit", "X-PP-User" : "user"])

        then:
        messageChain.receivedResponse.code.equals("413")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "DELETE",
                headers: ["X-PP-Groups" : "all-limit", "X-PP-User" : "user"])

        then:
        messageChain.receivedResponse.code.equals("413")
    }

    def "When rate limiting against multiple http methods in single rate limit line"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["X-PP-Groups" : "multi-limits", "X-PP-User" : "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "POST",
                headers: ["X-PP-Groups" : "multi-limits", "X-PP-User" : "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When rate limiting with 429 response code set"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/", method: "GET",
                headers: ["X-PP-Groups" : "multi2-limits", "X-PP-User" : "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/", method: "POST",
                headers: ["X-PP-Groups" : "multi2-limits", "X-PP-User" : "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("429")
    }


    // Helper methods
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

    private String getDefaultLimits() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + groupHeaderDefault + acceptHeaderDefault);

        return messageChain.receivedResponse.body
    }

    private void waitForLimitReset() {
        while (parseRemainingFromXML(getDefaultLimits(), 0) != parseAbsoluteFromXML(getDefaultLimits(), 0)) {
            sleep(10000)
        }
    }

    private void waitForAvailableRequest() {
        while (parseRemainingFromXML(getDefaultLimits(), 0) == 0) {
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

    private void useAllRemainingRequests(String user, String group, String path) {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-User" : user, "X-PP-Groups" : group]);

        while(!messageChain.receivedResponse.code.equals("413")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                    headers: ["X-PP-User" : user, "X-PP-Groups" : group]);
        }
    }
}
