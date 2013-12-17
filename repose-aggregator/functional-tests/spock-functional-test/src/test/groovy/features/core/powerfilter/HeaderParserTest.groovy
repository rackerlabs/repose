package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response


class HeaderParserTest extends ReposeValveTest {

    def static String locations = "/v1/queues/mqueue/messages?ids=locationOne,locationTwo"

    def setupSpec() {
        repose.applyConfigs( "features/core/powerfilter/common" )
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def "when expecting a comma-separated location header"() {
        given: "Origin service returns a comma-separated location header"
        def headerResp = {request -> return new Response(200, "OK",
                ["Location" : locations], "")}

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint,
                method: "GET",
                headers: ["x-test" : "test"],
                requestBody: "",
                defaultHandler:headerResp)

        then: "Repose returns a comma-separated location header"
        resp.getReceivedResponse().getHeaders().getFirstValue("Location").equals(locations)
    }

    def "when expecting a comma-separated header to be split"() {
        given: "Origin service returns a comma-separated header"
        def headerResp = {request -> return new Response(200, "OK",
                ["Allow" : "GET,POST"], "")}

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest(
                url:(String) reposeEndpoint,
                method: "GET",
                headers: ["x-test" : "test"],
                requestBody: "",
                defaultHandler: headerResp)

        then: "Repose returns multiple headers after splitting on commas"
        resp.getReceivedResponse().getHeaders().findAll("Allow").get(0).equalsIgnoreCase("GET")
        resp.getReceivedResponse().getHeaders().findAll("Allow").get(1).equalsIgnoreCase("POST")
    }

    def "when client sends a Location header with an un-escaped comma, then Repose should pass it through unchanged"() {

        def locations2 = "/path/to/resource?ids=valueOne,valueTwo"

        when: "Client sends Location header in a request with an un-escaped comma"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: ['Location': locations2])

        then: "Repose should pass the header to the origin service unchanged"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("Location") == 1
        mc.handlings[0].request.headers["Location"] == locations2

    }

    def "when client sends a Location header with an escaped comma, then Repose should pass it through unchanged"() {

        def locations2 = "/path/to/resource?ids=valueOne%2CvalueTwo"

        when: "Client sends Location header in a request with an un-escaped comma"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: ['Location': locations2])

        then: "Repose should pass the header to the origin service unchanged"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("Location") == 1
        mc.handlings[0].request.headers["Location"] == locations2

    }
}
