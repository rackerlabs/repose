package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class RequestQueryParamTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()

    }

    @Unroll("When client requests: #method #uriSuffixGiven, repose should normalize to: #uriSuffixExpected")
    def "when given a query param list, Repose should forward a valid query param list"() {

        when: "the client makes a request through Repose"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + uriSuffixGiven, method: method)


        then: "after passing through Repose, request path should contain a valid query param list"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path.endsWith(uriSuffixExpected)


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

    @Unroll("when given an improperly encoded URI character, Repose should properly encode it - #uriSuffixGiven")
    def "when given an improperly encoded URI character, Repose should properly encode it"() {

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uriSuffixGiven, method: method)

        then:
        messageChain.handlings.size() == 1
        messageChain.sentRequest.path.endsWith(uriSuffixGiven)
        messageChain.handlings[0].request.path.endsWith(uriSuffixExpected)

        where:
        uriSuffixGiven                             | uriSuffixExpected                          | method
        "/path/to/resource?key=value@%"            | "/path/to/resource?key=value%40%25"        | "GET"
        "/path/to/resource?key=va%lu@e"            | "/path/to/resource?key=va%25lu%40e"        | "GET"
        "/path/to/resource?key=value/othervalue"   | "/path/to/resource?key=value%2Fothervalue" | "GET"
        "/path/to/resource?key=value:value"        | "/path/to/resource?key=value%3Avalue"      | "GET"
        "/path/to/resource?key=value@value"        | "/path/to/resource?key=value%40value"      | "GET"
        "/path/to/resource?key=value?value"        | "/path/to/resource?key=value%3Fvalue"      | "GET"
        "/path/to/resource?key=value[value"        | "/path/to/resource?key=value%5Bvalue"      | "GET"
        "/path/to/resource?key=value]value"        | "/path/to/resource?key=value%5Dvalue"      | "GET"
        "/path/to/resource?key=value%2Fothervalue" | "/path/to/resource?key=value%2Fothervalue" | "GET"
        "/path/to/resource?key=value%3Avalue"      | "/path/to/resource?key=value%3Avalue"      | "GET"
        "/path/to/resource?key=value%40value"      | "/path/to/resource?key=value%40value"      | "GET"
        "/path/to/resource?key=value%3Fvalue"      | "/path/to/resource?key=value%3Fvalue"      | "GET"
        "/path/to/resource?key=value%5Bvalue"      | "/path/to/resource?key=value%5Bvalue"      | "GET"
        "/path/to/resource?key=value%5Dvalue"      | "/path/to/resource?key=value%5Dvalue"      | "GET"
        "/path/to/resource?key=value%20value"      | "/path/to/resource?key=value+value"        | "GET"
        "/path/to/resource?key=value+value"        | "/path/to/resource?key=value+value"        | "GET"
        "/path/to/resource?key=value%2Bvalue"      | "/path/to/resource?key=value%2Bvalue"      | "GET"
    }

    @Unroll("when given a URI that contains space, Repose should reject the request with a 400 - #uriSuffixGiven")
    def "when given a URI that contains space, Repose should reject the request with a 400"() {

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uriSuffixGiven, method: method)

        then:
        messageChain.handlings.size() == 0
        messageChain.sentRequest.path.endsWith(uriSuffixGiven)
        messageChain.receivedResponse.code == "400"

        where:
        uriSuffixGiven                      | method
        "/path/to/resource?key=value value" | "GET"
    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
