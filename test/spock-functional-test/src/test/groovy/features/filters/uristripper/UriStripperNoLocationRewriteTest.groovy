package features.filters.uristripper

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

class UriStripperNoLocationRewriteTest extends ReposeValveTest {

    def static String tenantId = "105620"
    def static originServiceEndpoint


    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/uristripper/common", "features/filters/uristripper/nolocationrewrite")
        repose.start()
        waitUntilReadyToServiceRequests()
        originServiceEndpoint = "${properties.getProperty("targetHostname")}:${properties.getProperty("targetPort")}"

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "when removing tenant id from request"() {

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint + "/v1/${tenantId}/path/to/resource"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Repose will send uri without tenant id"
        !((Handling) sentRequest).request.path.contains(tenantId)

    }

    def "when putting back tenant id to Location Header within the Response"() {

        given:
        def resp = { request -> return new Response(301, "Moved Permanently", ["Location": locationheader]) }

        when: "Request is sent through repose"
        def response = deproxy.makeRequest([url: reposeEndpoint + "/v1/${tenantId}/path/to/resource", defaultHandler: resp])
        def sentRequest = ((MessageChain) response).getHandlings()[0]

        then: "Repose will put back the tenant id in the location header"
        response.receivedResponse.headers.getFirstValue("Location").contains(tenantId) == containsTenant

        and: "Repose will send uri without tenant id"
        !((Handling) sentRequest).request.path.contains(tenantId)

        where:
        requestPath                        | locationheader                                         | containsTenant
        "/v1/${tenantId}/path/to/resource" | "http://${originServiceEndpoint}/v1/path/to/resource"  | false
        "/v1/${tenantId}/path/to/resource" | "http://${originServiceEndpoint}/v2/path/to/resource"  | false
        "/v1/${tenantId}/path/to/resource" | "http://${originServiceEndpoint}/v1/path/resource"     | false
        "/v1/${tenantId}/path/to/resource" | "http://${originServiceEndpoint}/no/relation/resource" | false


    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', headers: reqHeaders)
        def handling = mc.getHandlings()[0]

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

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', defaultHandler: xmlResp)
        def handling = mc.getHandlings()[0]

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
