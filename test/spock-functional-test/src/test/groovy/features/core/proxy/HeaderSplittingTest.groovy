package features.core.proxy
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

class HeaderSplittingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/proxy")
        repose.start()
    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }

    def "Should not split request headers according to rfc"() {
        given:
        def reqHeaders = ["user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36", "x-pp-user": "usertest1," +
                "usertest2, usertest3", "accept": "application/xml;q=1 , application/json;q=0.5"]

        when: "client passes a request through repose with headers"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint, headers: reqHeaders])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then:
        assert sentRequest.request.getHeaders().findAll("user-agent").size() == 1
        assert sentRequest.request.getHeaders().findAll("x-pp-user").size() == 4
        assert sentRequest.request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(200, "OK", respHeaders) }


        when: "client passes a request through repose with headers"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint, defaultHandler: xmlResp])

        then:
        assert respFromOrigin.receivedResponse.headers.findAll("location").size() == 1
        assert respFromOrigin.receivedResponse.headers.findAll("via").size() == 1
    }


}
