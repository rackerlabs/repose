package features.filters.ratelimiting

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

/*
 * Rate limiting tests ported over from python
 */
@Category(Slow.class)
class RateLimitingTest extends ReposeValveTest {
    static final handler = {return new Response(200, "OK")}
    static final Map<String, String> headers = ["X-PP-User" : "user"]

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

    def "When a limit has not been reached, request should pass"() {
        given: "the rate-limit has not been reached"
        waitForLimitReset()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        where:
        i << [0..3]
    }

    def "When a limit has been reached, request should not pass"() {
        given: "the rate-limit has been reached"
        useAllRemainingRequests()

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "When a limit is tested, method should not make a difference"() {
        given: "the rate-limit has not been reached"
        waitForLimitReset()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "PUT", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "DELETE", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "HEAD", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "When a limit has been reached, the limit should reset after one minute"() {
        given: "the limit has been reached"
        useAllRemainingRequests()

        when: "another request is sent"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0

        when: "a minute passes, and another request is sent"
        sleep(60000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler)

        then: "rate limit should have reset, and request should succeed"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
    }

    def "When Repose is configured with multiple nodes, rate-limiting info should be shared"() {
        given: "load the configs for multiple nodes, and wait until requests are available"
        repose.updateConfigs("features/filters/ratelimiting/twonodes/")
        useAllRemainingRequests()

        // TODO change reposeEndpoint to reposeEndpoint2
        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint.replaceFirst("8888", "8889"), method: "GET",
                headers: headers, defaultHandler: handler)

        then: "the request is rate-limited, and does not pass to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    private Long getLimits() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET");

//        TODO
//        parse body (xml) for remainingRequests: /limits/rates/rate/limit[1]/@remaining
//        return remainingRequests ? remainingRequests : -1;
    }

    private void waitForLimitReset() {
//        TODO
//        while (getLimits() == 0) {
//            sleep(10000);
//        }
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler);
        while(!messageChain.receivedResponse.code.equals("200")) {
            sleep(10000);
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                    defaultHandler: handler);
        }
    }

    private void useAllRemainingRequests() {
//        TODO
//        while (getLimits() > 0) {
//            MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET");
//            assert messageChain.receivedResponse.code.equals("200");
//            sleep(500);
//        }
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: handler);
        while(messageChain.receivedResponse.code.equals("200")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                    defaultHandler: handler);
        }
    }

// Start of JMeter port
//
//    def "When rate limiting requests, should limit requests when user has no requests remaining"() {
//        waitForLimitReset();
//        useAllRemainingRequests();
//
//        when:
//        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
//                headers: ["X-PP-Groups" : "customer", "Accept" : "application/xml"])
//
//        then:
//        messageChain.receivedResponse.code.equals("413")
//        //messageChain.receivedResponse.body.equals("retryAfter=\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z\"")
//    }
//
//    private Long getLimits() {
//        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET");
//
//        // parse body (xml) for remainingRequests: /limits/rates/rate/limit[1]/@remaining
//        // return remainingRequests ? remainingRequests : -1;
//    }
//
//    private Long getNamedLimit() {
//        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET");
//
//        // parse body (xml) for remainingRequests: /limits/rates/rate[@regex='${regex}']/limit/@remaining
//        // return remainingRequests ? remainingRequests : -1;
//    }
//
//    private void waitForLimitReset() {
//        while (getLimits() == 0) {
//            sleep(10000);
//        }
//    }
//
//    private void waitForNamedLimitReset() {
//        while (getNamedLimit() == 0) {
//            sleep(10000);
//        }
//    }
//
//    private void useAllRemainingRequests() {
//        while (getLimits() > 0) {
//            MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET");
//            assert messageChain.receivedResponse.code.equals("200");
//            sleep(500);
//        }
//    }
//
//    private void useAllRemainingRequestsCustomURI() {
//        while (getNamedLimit() > 0) {
//            MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET");
//            assert messageChain.receivedResponse.code.equals("200");
//            sleep(500);
//        }
//    }
//
//    def "When rate limiting requests, should allow requests when user has requests remaining"() {
//        // Wait until user has available requests
//
//        when:
//        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
//                headers: ["X-PP-Groups" : "customer", "Accept" : "application/xml"])
//
//        then:
//        messageChain.receivedResponse.code.equals("200")
//    }
//
//    def "When rate limiting requests with multiple X-PP-User values, should allow requests with new username"() {
//        // Wait until user has available requests
//        // Make requests while user has requests remaining
//
//        when:
//        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
//                headers: ["X-PP-Groups" : "customer", "Accept" : "application/xml", "X-PP-User" : "that_other_user;q=1.0"])
//
//        then:
//        messageChain.receivedResponse.code.equals("200")
//    }
//
//    def "When rate limiting requests with multiple X-PP-Group values, should allow requests with new group with higher priority"() {
//        // Wait until user has available requests
//        // Make requests while user has requests remaining
//
//        when:
//        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
//                headers: ["X-PP-Groups" : "customer;q=0.5,higher;q=0.75", "Accept" : "application/xml"])
//
//        then:
//        messageChain.receivedResponse.code.equals("200")
//    }
//
//    def "When requesting rate limits as xml for limited group, should receive xml rate limits for Accept application/xml"() {
//        when:
//        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
//                headers: ["X-PP-Groups" : "customer;q=1.0", "Accept" : "application/xml", "X-PP-User" : "user"])
//
//        then:
//        messageChain.receivedResponse.code.equals("200")
//        //messageChain.receivedResponse.headers.findAll("Content-Encoding").contains("application/xml")
//        messageChain.receivedResponse.body.length() > 0
//        messageChain.receivedResponse.body.contains("/limits/rates/rate/limit")
//        messageChain.receivedResponse.body.contains("/limits/absolute/limit")
//    }
//
//    def "When requesting rate limits as xml for limited group, should receive xml rate limits for .xml"() {
//
//    }
//
//    def "When requesting rate limits as xml for limited group, should receive xml rate limits for .xml with Accept application/json"() {
//
//    }
//
//    def "When requesting rate limits as xml for limited group, should receive xml rate limits for .xml with Accept application/unknown"() {
//
//    }
//
//    def "When requesting rate limits as xml for unlimited group, Should receive xml rate limits for Accept application/xml"() {
//
//    }
//
//    def "When requesting rate limits as xml for unlimited group, Should receive xml rate limits for .xml"() {
//
//    }
//
//    def "When requesting rate limits as xml for unlimited group, Should receive xml rate limits for .xml with Accept application/json"() {
//
//    }
//
//    def "When requesting rate limits as xml for unlimited group, Should receive xml rate limits for .xml with Accept application/unknown"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits for Accept application/json"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits with no Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits with empty Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits for Accept */*"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits for .json"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits for .json with Accept application/xml"() {
//
//    }
//
//    def "When requesting rate limits as json for limited group, Should receive json rate limits for .json with Accept application/unknown"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits with no Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits with empty Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits for Accept */*"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits for .json"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits for .json with Accept application/xml"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits for .json with Accept application/unknown"() {
//
//    }
//
//    def "When requesting rate limits as json for unlimited group, Should receive json rate limits for Accept application/json"() {
//
//    }
//
//    def "When requesting rate limits with invalid Accept header, Should receive 406 response when invalid Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters with no Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters with empty Accept header"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters for Accept */*"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters for .json"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters for .json with Accept application/xml"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits with special characters for .json with Accept application/unknown"() {
//
//    }
//
//    def "When requesting rate limits as json for group with special characters, Should receive json rate limits for Accept application/json with special characters"() {
//
//    }
//
//    def "When rate limiting against multiple regexes, Should limit requests when user has no requests remaining"() {
//
//    }
//
//    def "When rate limiting against multiple regexes, Should not limit requests against a different regex"() {
//
//    }
//
//    def "When rate limiting against a configured limit with http-method of ALL, Should return 200 when limits remain for configured ALL http-methods"() {
//
//    }
//
//    def "When rate limiting against a configured limit with http-method of ALL, Should return limits for ALL http-method"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line, Should return 200 when limits remain for multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line, Should return 413 when making a POST after GET's have executed against multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line with 429 response code set, Should return 200 when limits remain for multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line with 429 response code set, Should return 429 when making a POST after GET's have executed against multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line with capture groups false, Should return 200 when limits remain for multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting against multiple http methods in single rate limit line with capture groups false, Should return 429 when making a POST after GET's have executed against multiple http methods in single line"() {
//
//    }
//
//    def "When rate limiting /limits, Should return 200 when limits remain for /limits"() {
//
//    }
//
//    def "When rate limiting /limits, Should return 413 when making a GET after initial GET has executed against /limits"() {
//
//    }
}
