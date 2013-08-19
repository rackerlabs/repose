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
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

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
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

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
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

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
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

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
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b"]
    }

    def "when translating request headers many-to-many"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/manyToMany" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().contains("X-Header-D")
        sentRequest.request.getHeaders().contains("X-Header-E")
        sentRequest.request.getHeaders().contains("X-Header-F")
        sentRequest.request.getHeaders().getFirstValue("X-Header-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("c")
        sentRequest.request.getHeaders().getFirstValue("X-Header-D").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-E").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-F").equalsIgnoreCase("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
    }

    def "when translating request headers many-to-one"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/manyToOne" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-D")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
    }

    def "when translating request headers translating to existing header"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/translatingToExistingHeader" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-Existing")
        sentRequest.request.getHeaders().getFirstValue("X-Header-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("a")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("b")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-Existing" : "b"]
        "GET"  | ["X-Header-A" : "a", "X-Header-Existing" : "b"]
    }

    def "when translating request headers with mixed case"() {
        setup: "load the correct configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/caseSensitivityHeaders" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Header-A")
        sentRequest.request.getHeaders().contains("X-Header-B")
        sentRequest.request.getHeaders().contains("X-Header-C")
        sentRequest.request.getHeaders().contains("X-Header-D")
        sentRequest.request.getHeaders().contains("X-Header-E")
        sentRequest.request.getHeaders().getFirstValue("X-Header-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-C").equalsIgnoreCase("c")
        sentRequest.request.getHeaders().getFirstValue("X-Header-D").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().findAll("x-header-e").contains("b")
        sentRequest.request.getHeaders().findAll("x-header-e").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
    }


    def "when translating CSL request headers"() {
        setup: "load the csl configuration file"
        repose.applyConfigs( "features/filters/headertranslation/common",
                "features/filters/headertranslation/csl" )
        repose.start()

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest((String) reposeEndpoint, method, reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("x-rax-username")
        sentRequest.request.getHeaders().contains("x-rax-tenants")
        sentRequest.request.getHeaders().contains("x-rax-roles")
        sentRequest.request.getHeaders().contains("x-pp-user")
        sentRequest.request.getHeaders().contains("x-tenant-name")
        sentRequest.request.getHeaders().contains("x-roles")

        where:
        method | reqHeaders
        "POST" | ["x-pp-user" : "a", "x-tenant-name" : "b", "x-roles" : "c"]
        "GET"  | ["x-pp-user" : "a", "x-tenant-name" : "b", "x-roles" : "c"]
    }
}
