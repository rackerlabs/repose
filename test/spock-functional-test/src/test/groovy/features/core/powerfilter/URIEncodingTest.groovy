package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

class URIEncodingTest extends ReposeValveTest {

    def static String parameterWithSpace = "/messages?ids= locationOne"
    def static String parameterWithSpaceWithPlus = "/messages?ids=+locationOne"

    def static String URIWithSpace = "/%20messages?ids=locationOne"
    def static String URIWithSpaceAsciiEncode = "/%20messages?ids=+locationOne"


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

   /* def "when space in the parameter value"() {
        given:
        def headerResp = {request -> return new Response(200, "OK",
                ["Location" : parameterWithSpace], "")}

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint+parameterWithSpace , "GET", ["x-test" : "test"], "", headerResp)

        then: "Repose returns a comma-separated location header"
        resp.getReceivedResponse().getHeaders().getFirstValue("Location").equals(parameterWithSpaceWithPlus)
    }
    */
    def "when space in the URI"() {
        given:
        def headerResp = {request -> return new Response(200, "OK",
                ["Location" : URIWithSpace], "")}

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest((String) reposeEndpoint+URIWithSpace , "GET", ["x-test" : "test"], "", headerResp)

        then: "Repose returns a comma-separated location header"
        messageChain.sentRequest.path==URIWithSpace

    }

    def "when Plus in the URI"() {
        given:
        def headerResp = {request -> return new Response(200, "OK",
                ["Location" : URIWithPlus], "")}
        def String URIWithPlus = URLEncoder.encode("+messages","UTF-8");
        def String URIWithPlusAsciiEncode = "/%2Bmessages?ids=+locationOne"

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest((String) reposeEndpoint+URIWithPlus , "GET", ["x-test" : "test"], "", headerResp)

        then: "Repose returns a comma-separated location header"
        resp.getReceivedResponse().getHeaders().getFirstValue("Location").equals(URIWithPlusAsciiEncode)
    }
}