package features.filters.apivalidator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll
/*
 * Api validator tests ported over from and JMeter
 */
class ApiValidatorTest extends ReposeValveTest{

    private final String baseGroupPath = "/wadl/group1"
    private final String baseDefaultPath = "/wadl/default"

    private final Map<String, String> defaultHeaders = [
            "Accept" : "application/xml",
            "Host"   : "localhost",
            "Accept-Encoding" : "identity",
            "User-Agent" : "gdeproxy"
    ]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/jmeter/")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Happy path: when no role passed, should get default wadl - #request")
    def "Happy path: when no role passed, should get default wadl"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain
        def customHandler = {return new Response(200, "OK", [], reqBody)}

        when: "When Requesting " + method + " " + request
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                request, method: method, headers: defaultHeaders,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then: "result should be " + responseCode
        messageChain.receivedResponse.code.equals(responseCode)

//        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        where:
        responseCode | request                                                | method | reqBody
        "200"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"   | "GET"  | ""
        "404"        | "/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"  | "GET"  | ""
        "405"        | "/resource1/id"                                        | "POST" | ""
        "415"        | "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"   | "PUT"  | "some data"

    }

    def "Happy path: when Group Passed, Should Get Role Specific WADL"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles" : "group1", "Content-Type" : "application/xml"]

        when: "When Requesting resource with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: defaultHeaders + headers)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")

        when: "When Requesting invalid resource with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1x/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: defaultHeaders + headers)

        then: "should return not found"
        messageChain.receivedResponse.code.equals("404")
        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using invalid method with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id", method: "POST", headers: defaultHeaders + headers)

        then: "should return not found"
        messageChain.receivedResponse.code.equals("405")
        messageChain.receivedResponse.body.contains("XML Not Authorized... Syntax highlighting is magical.")

        when: "When using valid media type with x-roles"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "POST", headers: defaultHeaders + headers,
                requestBody: "<c xmlns='http://test.openrespose/test/v1.1'><test>some data</test></c>")

        then: "should return OK"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("some data")
    }

    def "Happy path: when Ignore XSD Extension enabled"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles" : "default", "Content-Type" : "application/xml"]

        when: "When Requesting with valid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")

        when: "When Requesting with invalid content and Ignore XSD enabled"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node2 id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node2 hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")
    }

    def "Happy path: When Ignore XSD Extension disabled"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["X-Roles" : "default2", "Content-Type" : "application/xml"]

        when: "When Requesting with valid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
        // Origin service handler does not return a body
        // messageChain.receivedResponse.body.contains("<node hostname=\"localhost\" http-port=\"8088\" id=\"proxy-n01\"/>")

        when: "When Requesting with invalid content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node2 id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></nodeList></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("One of '{\"http://test.openrespose/test/v1.1\":node}' is expected")

        when: "When Requesting with non well-formed content"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource2/unvalidated/echobody", method: "PUT", headers: defaultHeaders + headers,
                requestBody: "<c xmlns=\"http://test.openrespose/test/v1.1\"><nodeList><node id=\"proxy-n01\" hostname=\"localhost\" http-port=\"8088\"  /></c>")

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("The element type \"nodeList\" must be terminated by the matching end-tag")
    }

    def "Happy path: When Passing to resource with required header"() {
        setup: "declare messageChain to be of type MessageChain, additional headers"
        MessageChain messageChain
        Map<String, String> headers = ["x-required-header" : "somevalue"]

        when: "When Requesting default resource with no roles and required header"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource1/id/reqheader", method: "GET", headers: defaultHeaders + headers)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("200")
    }

    def "Unhappy path: When Passing to resource without required header"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain

        when: "When Requesting default resource with no roles without required header"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + baseDefaultPath +
                "/resource1/id/reqheader", method: "GET", headers: defaultHeaders)

        then: "should return resource"
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.body.contains("Expecting an HTTP header x-required-header")
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
                    "X-Roles" : "group1"
            ]

        when: "When Requesting resource with x-roles"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: reqHeaders)
        def handling = messageChain.getHandlings()[0]

        then:
        handling.request.getHeaders().findAll("user-agent").size() == 1
        handling.request.headers['user-agent'] == userAgentValue
        handling.request.getHeaders().findAll("x-pp-user").size() == 3
        handling.request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders, "") }
        Map<String, String> headers = ["X-Roles" : "group1", "Content-Type" : "application/xml"]


        when: "client passes a request through repose with headers"
        def MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + baseGroupPath +
                "/resource1/id/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", method: "GET", headers: headers,
                defaultHandler: xmlResp)

        then:
        messageChain.receivedResponse.headers.findAll("location").size() == 1
        messageChain.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        messageChain.receivedResponse.headers.findAll("via").size() == 1
    }
}
