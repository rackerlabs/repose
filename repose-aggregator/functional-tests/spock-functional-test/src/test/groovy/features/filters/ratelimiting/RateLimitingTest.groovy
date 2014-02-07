package features.filters.ratelimiting

import framework.ReposeValveTest
import framework.category.Slow
import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.w3c.dom.Document
import org.xml.sax.InputSource
import spock.lang.Unroll

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Rate limiting tests ported over from python and JMeter
 */
class RateLimitingTest extends ReposeValveTest {
    final handler = {return new Response(200, "OK")}

    final Map<String, String> userHeaderDefault = ["X-PP-User" : "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups" : "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept" : "application/xml"]

    final def absoluteLimitResponse = {
        return new Response("200",
                "OK", ["Content-Type" : "application/xml"],
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<limits xmlns=\"http://docs.openstack.org/common/api/v1.0\"><absolute>" +
                        "<limit name=\"Admin\" value=\"15\"/><limit name=\"Tech\" value=\"10\"/>" +
                        "<limit name=\"Demo\" value=\"5\"/></absolute></limits>") }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/onenodes", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def cleanup() {
        waitForLimitReset()
    }

    def "When a limit is tested, method should not make a difference"() {
        given: "the rate-limit has not been reached"
        waitForLimitReset()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "PUT",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

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
        useAllRemainingRequests("user","all-limits-small","/service/limits")

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    @Category(Slow.class)
    def "When a limit has been reached, the limit should reset after one minute"() {
        given: "the limit has been reached"
        useAllRemainingRequests("user","all-limits-small","/service/limits")

        when: "another request is sent"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0

        when: "a minute passes, and another request is sent"
        sleep(60000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "rate limit should have reset, and request should succeed"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
    }

    def "When rate limiting requests with multiple X-PP-User values, should allow requests with new username"() {
        given: "the limit has been reached for the default user"
        useAllRemainingRequests("user","all-limits-small","/service/limits")

        when: "a request is made by a different user"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-User" : "that_other_user;q=1.0"] + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting requests with multiple X-PP-Group values, should allow requests with new group with higher priority"() {
        given: "the limit has been reached for a user in a certain group"
//        useAllRemainingRequests("user","customer","/service/test")
        MessageChain messageChain = null;

        for(x in 0..3){
            deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                    headers: userHeaderDefault + ["X-PP-Groups" : "customer"])

        }
        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups" : "customer"])

        then:
        messageChain.receivedResponse.code.equals("413")

        when: "a request is made using a new group with a higher quality"
         messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups" : "customer;q=0.5,higher;q=0.75"])

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "a request is made using a new group with a higher and lower quality"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups" : "customer;q=0.5,high;q=0.75,lower;q=0.0,higher;q=0.9,other;q=0.6,none"])

        then: "the request should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    @Unroll("when requesting rate limits for unlimited groups with #acceptHeader ")
    def "When requesting rate limits for unlimited groups, should receive rate limits in request format"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups" : "unlimited;q=1.0", "X-PP-User" : "unlimited-user"] + acceptHeader,
                defaultHandler: absoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code.equals("200")
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)
        messageChain.receivedResponse.body.length() > 0
        if(expectedFormat.contains("xml")){
            assert parseRateCountFromXML(messageChain.receivedResponse.body) == 0
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 0) == 15
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 1) == 10
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 2) == 5
        }
        else{
            assert parseRateCountFromJSON(messageChain.receivedResponse.body) == 0
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Admin") == 15
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Tech") == 10
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Demo") == 5
        }

        where:
        path                   | acceptHeader                       | expectedFormat
        "/service/limits"      | ["Accept" : "application/xml"]     | "application/xml"
        "/service/limits"      | ["Accept" : "application/json"]    | "application/json"
        "/service/limits"      | []                                 | "application/json"
        "/service/limits"      | ["Accept" : ""]                    | "application/json"
        "/service/limits"      | ["Accept" : "*/*"]                 | "application/json"
    }

    @Unroll("when requesting rate limits for limited groups with #acceptHeader ")
    def "When requesting rate limits for limited groups, should receive rate limits in request format"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups" : "customer;q=1.0", "X-PP-User" : "user"] + acceptHeader,
                defaultHandler: absoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code.equals("200")
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)
        messageChain.receivedResponse.body.length() > 0
        if(expectedFormat.contains("xml")){
            assert parseRateCountFromXML(messageChain.receivedResponse.body) == 0
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 0, true) == 15
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 1, true) == 10
            assert parseAbsoluteFromXML(messageChain.receivedResponse.body, 2, true) == 5
        }
        else{
            assert parseRateCountFromJSON(messageChain.receivedResponse.body) > 0
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Admin") == 15
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Tech") == 10
            assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Demo") == 5
        }

        where:
        path                   | acceptHeader                       | expectedFormat
        "/service/limits"      | ["Accept" : "application/xml"]     | "application/xml"
        "/service/limits"      | ["Accept" : "application/json"]    | "application/json"
        "/service/limits"      | []                                 | "application/json"
        "/service/limits"      | ["Accept" : ""]                    | "application/json"
        "/service/limits"      | ["Accept" : "*/*"]                 | "application/json"
    }

    def "When requesting rate limits  invalid Accept header, Should receive 406 response when invalid Accept header"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-Groups" : "customer;q=1.0", "X-PP-User" : "user", "Accept" : "application/unknown"])

        then:
        messageChain.receivedResponse.code.equals("406")
    }

    @Unroll("When requesting rate limits for group with special characters with #acceptHeader ")
    def "When requesting rate limits as json for group with special characters"(){
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-Groups" : "unique;q=1.0", "X-PP-User" : "user"] + acceptHeader,
                defaultHandler: absoluteLimitResponse
        )

        then:
        messageChain.receivedResponse.code.equals("200")
        messageChain.receivedResponse.headers.findAll("Content-Type").contains(expectedFormat)
        messageChain.receivedResponse.body.length() > 0
        assert parseRateCountFromJSON(messageChain.receivedResponse.body) > 0
        assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Admin") == 15
        assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Tech") == 10
        assert parseAbsoluteFromJSON(messageChain.receivedResponse.body, "Demo") == 5
        assert messageChain.receivedResponse.body.contains("service/\\\\w*")
        assert messageChain.receivedResponse.body.contains("service/\\\\s*")
        assert messageChain.receivedResponse.body.contains("service/(\\\".+\\")
        assert messageChain.receivedResponse.body.contains("service/\\\\d*")

        where:
        path                   | acceptHeader                       | expectedFormat
        "/service/limits"      | ["Accept" : "application/json"]    | "application/json"
        "/service/limits"      | []                                 | "application/json"
        "/service/limits"      | ["Accept" : ""]                    | "application/json"
        "/service/limits"      | ["Accept" : "*/*"]                 | "application/json"
    }

    def "When rate limiting against multiple regexes, Should not limit requests against a different regex"() {
        given:
        useAllRemainingRequests("user", "multiregex", "/service/endpoint1")

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/endpoint1", method: "GET",
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
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/all", method: "POST",
                headers: ["X-PP-Groups" : "all-limits", "X-PP-User" : "123ALL"])

        then:
        messageChain.receivedResponse.code.equals("200")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "DELETE",
                headers: ["X-PP-Groups" : "all-limit", "X-PP-User" : "user"])

        then:
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting against multiple http methods in single rate limit line"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: ["X-PP-Groups" : "multi-limits", "X-PP-User" : "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: ["X-PP-Groups" : "multi-limits", "X-PP-User" : "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When rate limiting with 429 response code set"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "GET",
                headers: ["X-PP-Groups" : "multi2-limits", "X-PP-User" : "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "POST",
                headers: ["X-PP-Groups" : "multi2-limits", "X-PP-User" : "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("429")
    }

    def "When rate limiting with 429 response code set with capture groups false"(){
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "GET",
                headers: ["X-PP-Groups" : "multi3-limits", "X-PP-User" : "429User"] + acceptHeaderDefault)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all1", method: "GET",
                headers: ["X-PP-Groups" : "multi3-limits", "X-PP-User" : "429User"] + acceptHeaderDefault)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all2", method: "GET",
                headers: ["X-PP-Groups" : "multi3-limits", "X-PP-User" : "429User"] + acceptHeaderDefault)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "POST",
                headers: ["X-PP-Groups" : "multi3-limits", "X-PP-User" : "429User"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("429")
    }

    def "When rate limiting /limits"(){
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-Groups" : "query-limits", "X-PP-User" : "123limits"] + acceptHeaderDefault)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-Groups" : "query-limits", "X-PP-User" : "123limits"] + acceptHeaderDefault)

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5",
                    "x-pp-groups": "unlimited"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }
        Map<String, String> headers = ["x-pp-user": "usertest1, usertest2, usertest3", "X-PP-Groups" : "unlimited",
                "Content-Type" : "application/xml"]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers,
                defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
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

    private int parseAbsoluteFromXML(String s, int limit, boolean useSlurper) {
        if(!useSlurper)
            return parseAbsoluteFromXML(s, limit)
        else{
            def xml = XmlSlurper.newInstance().parseText(s)
            return Integer.parseInt(xml.children()[1][0].children()[limit].attributes()["value"])
        }
    }

    private int parseAbsoluteFromJSON(String body, String limit){
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.absolute[limit].value
    }



    private int parseRemainingFromJSON(String body, String limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.absolute[limit].value
    }

    private int parseRateCountFromXML(String body){
        def xml = XmlSlurper.newInstance().parseText(body)
        return xml.limits.rates.size()
    }


    private int parseRateCountFromJSON(String body){
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate.size()
    }

    private String getDefaultLimits(Map group = null) {
        def groupHeader = (group != null) ? group : groupHeaderDefault
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + groupHeader + acceptHeaderDefault);

        return messageChain.receivedResponse.body
    }

    private void waitForLimitReset(Map group = null) {
        while (parseRemainingFromXML(getDefaultLimits(group), 0) != parseAbsoluteFromXML(getDefaultLimits(group), 0)) {
            sleep(1000)
        }
    }

    private void waitForAvailableRequest() {
        while (parseRemainingFromXML(getDefaultLimits(), 0) == 0) {
            sleep(1000)
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
