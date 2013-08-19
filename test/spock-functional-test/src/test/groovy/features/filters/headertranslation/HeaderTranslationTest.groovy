package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.HeaderCollection
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Request

class HeaderTranslationTest extends ReposeValveTest {

    def setup() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        repose.stop()
        deproxy.shutdown()
    }

    def "when translating request headers one-to-one without removal"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                             "features/filters/headertranslation/oneToOne" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().getFirstValue("X-Header-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    def "when translating request headers one-to-one with removal"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                             "features/filters/headertranslation/oneToOneRemoval" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    def "when translating request headers one-to-many without removal"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                             "features/filters/headertranslation/oneToMany" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().contains("X-Header-D")
        sentRequest.request.getHeaders().getFirstValue("X-Header-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-D").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    def "when translating request headers one-to-many with removal"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                             "features/filters/headertranslation/oneToManyRemoval" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().contains("X-Header-D")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-D").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    def "when translating request headers one-to-none"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                             "features/filters/headertranslation/stripHeader" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

}
