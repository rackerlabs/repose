package features.filters.uriStripper.locationRewrite

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

class UriStripperLocationRewriteTest extends ReposeValveTest {

    def static String tenantId = "105620"
    def static originServiceEndpoint

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/uriStripper/common", "features/filters/uriStripper/noLocationRewrite")
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

    @Unroll("Location header should be changed when repose can find a place to place removed token: request path: #requestPath location header: #locationheader contains tenant: #containsTenant")
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
        requestPath                               | locationheader                                                              | containsTenant
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource"                       | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v2/path/to/resource"                       | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/resource"                          | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource?a=b"                   | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/to/resource?a=b,c,d,e"             | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v2/path/to/resource?a=b"                   | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/path/resource?a=b"                      | true
        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/v1/////path////to////resource"             | true
        "/v1////${tenantId}/path/to///resource"   | "http://${originServiceEndpoint}/v1/path/to/resource"                       | true

        "/v1/${tenantId}/path/////to////resource" | "/v1/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v2/path/to/resource"                                                      | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/resource"                                                         | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource?a=b"                                                  | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/to/resource?a=b,c,d,e,f"                                          | true
        "/v1/${tenantId}/path/to/resource"        | "/v2/path/to/resource?a=b"                                                  | true
        "/v1/${tenantId}/path/to/resource"        | "/v1/path/resource?a=b"                                                     | true

        "/v1/${tenantId}/path/to/resource"        | "http://${originServiceEndpoint}/no/relation/resource"                      | false

        "/v1/${tenantId}/path/to/resource"        | "httdfjklsajfkdsfp://${originServiceEndpoint}/v1/path/to/resource/#\$%^&*(" | false


    }
}
