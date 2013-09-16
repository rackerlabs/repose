package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class RequestQueryParamTest extends ReposeValveTest {

    def setupSpec() {
        repose.applyConfigs( "features/core/proxy" )
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def setup() {
    }

    def cleanup() {
    }

    @Unroll("When client requests: #uriSuffixGiven, repose should normalize to: #uriSuffixExpected")
    def "when given a query param list, Repose should forward a valid query param list"() {

        when: "the client makes a request through Repose"
        def MessageChain messageChain = deproxy.makeRequest(reposeEndpoint + uriSuffixGiven, method)
        def Handling handling = messageChain.getHandlings().get(0)

        then: "after passing through Repose, request path should contain a valid query param list"
        messageChain.getHandlings().size() == 1
        handling.request.path.endsWith(uriSuffixExpected)

        where: "given a path with query params defined"
        uriSuffixGiven                       | uriSuffixExpected                   | method
        "/path/to/resource?"                 | "/path/to/resource"                 | "GET"
        "/path/to/resource?"                 | "/path/to/resource"                 | "POST"
        "/path/to/resource?="                | "/path/to/resource"                 | "GET"
        "/path/to/resource?="                | "/path/to/resource"                 | "POST"
        "/path/to/resource?&"                | "/path/to/resource"                 | "GET"
        "/path/to/resource?&"                | "/path/to/resource"                 | "POST"
        "/path/to/resource?=&"               | "/path/to/resource"                 | "GET"
        "/path/to/resource?=&"               | "/path/to/resource"                 | "POST"
        "/path/to/resource?=&="              | "/path/to/resource"                 | "GET"
        "/path/to/resource?=&="              | "/path/to/resource"                 | "POST"
        "/path/to/resource?&=&"              | "/path/to/resource"                 | "GET"
        "/path/to/resource?&=&"              | "/path/to/resource"                 | "POST"
        "/path/to/resource?a=12345"          | "/path/to/resource?a=12345"         | "GET"
        "/path/to/resource?a=12345"          | "/path/to/resource?a=12345"         | "POST"
        "/path/to/resource?&a=12345"         | "/path/to/resource?a=12345"         | "GET"
        "/path/to/resource?&a=12345"         | "/path/to/resource?a=12345"         | "POST"
        "/path/to/resource/?&a=12345"        | "/path/to/resource/?a=12345"        | "GET"
        "/path/to/resource/?&a=12345"        | "/path/to/resource/?a=12345"        | "POST"
        "/path/to/resource?&a=12345&b=54321" | "/path/to/resource?a=12345&b=54321" | "GET"
        "/path/to/resource?&a=12345&b=54321" | "/path/to/resource?a=12345&b=54321" | "POST"
    }
}
