package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.HeaderCollection
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Request

class HeaderTranslationTest extends ReposeValveTest {

    def setupSpec() {
        repose.applyConfigs( "features/filters/headertranslation" )
        repose.start()
    }

    def setup() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        deproxy.shutdown()
    }

    def cleanupSpec() {
        repose.stop()
    }

    def "when translating request headers one-to-one with removal"() {
        //TODO setup: "load the correct configuration file"

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    /*def "when translating request headers one-to-one without removal"() {

    }

    def "when translating request headers one-to-many with removal"() {

    }

    def "when translating request headers one-to-many without removal"() {

    }

    def "when translating request headers one-to-none without removal"() {

    }

    def "when translating request headers one-to-none without removal"() {

    }*/

}
