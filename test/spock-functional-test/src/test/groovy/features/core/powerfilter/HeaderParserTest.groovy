package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response


class HeaderParserTest extends ReposeValveTest {

    def static String locations = "/v1/queues/mqueue/messages?ids=locationOne,locationTwo"

    def setupSpec() {
        repose.applyConfigs( "features/core/powerfilter/common" )
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
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
        def resp = deproxy.makeRequest((String) reposeEndpoint, "GET", ["test" : "test"], "", headerResp)

        then: "Repose returns a comma-separated location header"
        resp.getReceivedResponse().getHeaders().getFirstValue("Location").equals(locations)
    }

    def "when expecting a comma-separated header to be split"() {
        given: "Origin service returns a comma-separated header"
        def headerResp = {request -> return new Response(200, "OK",
                ["Multiple-Headers" : "one,two,three"], "")}

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint, "GET", ["test" : "test"], "", headerResp)

        then: "Repose returns multiple headers after splitting on commas"
        resp.getReceivedResponse().getHeaders().getFirstValue("Multiple-Headers").equals("one")
    }
}
