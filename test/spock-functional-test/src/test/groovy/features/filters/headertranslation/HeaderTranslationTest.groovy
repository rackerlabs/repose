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

    def "when translating request headers one-to-one with multiple values with removal"() {
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
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d"]
        "GET"  | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d"]
    }

    def "when translating request headers one-to-one with multiple values without removal"() {
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
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d"]
        "GET"  | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d"]
    }

    def "when translating request headers with multiple values one-to-many without removal"() {
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
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a, b, c", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a, b, c", "X-Header-B" : "b"]
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

        and: "origin receives headers which should not be affected by the translation"
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Header-B").equalsIgnoreCase("b")

        and: "origin receives translated all header values"
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("c")

        and: "origin receives translated header values in order in which they were sent"
        sentRequest.request.getHeaders().findAll("X-Header-C") == ["a","b","c"]
        sentRequest.request.getHeaders().findAll("X-Header-D") == ["a","b","c"]


        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a,b,c", "X-Header-B" : "b"]
        "GET"  | ["X-Header-A" : "a,b,c", "X-Header-B" : "b"]
        "POST" | ["X-Header-A" : "a ,b,c,,", "X-Header-B" : "b"]

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
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-E").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-F").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
        "GET"  | ["X-Header-A" : "a", "X-Header-B" : "b", "X-Header-C" : "c"]
    }


    def "when translating request headers with multiple values many-to-many"() {
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
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-Header-E").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-F").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("b")
        sentRequest.request.getHeaders().findAll("X-Header-E").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-F").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-A").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-B").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-C").contains("e")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("c")
        sentRequest.request.getHeaders().findAll("X-Header-E").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-F").contains("e")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d", "X-Header-C" : "c, d ,e"]
        "GET"  | ["X-Header-A" : "a, b, c", "X-Header-B" : "b, c, d", "X-Header-C" : "c, d ,e"]
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

    def "when translating request headers with multiple values many-to-one"() {
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
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("d")
        sentRequest.request.getHeaders().findAll("X-Header-D").contains("e")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a,b,c", "X-Header-B" : "b,c,d", "X-Header-C" : "c,d,e"]
        "GET"  | ["X-Header-A" : "a,b,c", "X-Header-B" : "b,c,d", "X-Header-C" : "c,d,e"]
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

    def "when translating request headers translating to existing header with multiple values"() {
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
        sentRequest.request.getHeaders().findAll("x-header-a").contains("a")
        sentRequest.request.getHeaders().findAll("x-header-a").contains("c")
        sentRequest.request.getHeaders().findAll("x-header-a").contains("e")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("a")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("b")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("c")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("d")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("e")

        where:
        method | reqHeaders
        "POST" | ["X-Header-A" : "a, c, e", "X-Header-Existing" : "b, d"]
        "GET"  | ["X-Header-A" : "a, c, e", "X-Header-Existing" : "b, d"]
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
