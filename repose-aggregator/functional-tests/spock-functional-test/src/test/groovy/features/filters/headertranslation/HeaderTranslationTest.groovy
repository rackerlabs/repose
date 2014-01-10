package features.filters.headertranslation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class HeaderTranslationTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headertranslation/common", params)
        repose.start()
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers one-to-one without removal"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-OneToOne-A")
        sentRequest.request.getHeaders().contains("X-OneToOne-B")
        sentRequest.request.getHeaders().contains("X-OneToOne-C")
        sentRequest.request.getHeaders().getFirstValue("X-OneToOne-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-OneToOne-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToOne-C").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-OneToOne-A" : "a", "X-OneToOne-B" : "b"]
        "GET"  | ["X-OneToOne-A" : "a", "X-OneToOne-B" : "b"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers one-to-one with removal"() {
        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-OneToOneRemoval-A")
        sentRequest.request.getHeaders().contains("X-OneToOneRemoval-B")
        sentRequest.request.getHeaders().contains("X-OneToOneRemoval-C")
        sentRequest.request.getHeaders().getFirstValue("X-OneToOneRemoval-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToOneRemoval-C").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-OneToOneRemoval-A" : "a", "X-OneToOneRemoval-B" : "b"]
        "GET"  | ["X-OneToOneRemoval-A" : "a", "X-OneToOneRemoval-B" : "b"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers one-to-many without removal"() {
        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-OneToMany-A")
        sentRequest.request.getHeaders().contains("X-OneToMany-B")
        sentRequest.request.getHeaders().contains("X-OneToMany-C")
        sentRequest.request.getHeaders().contains("X-OneToMany-D")
        sentRequest.request.getHeaders().getFirstValue("X-OneToMany-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-OneToMany-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToMany-C").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-OneToMany-D").equalsIgnoreCase("a")

        where:
        method | reqHeaders
        "POST" | ["X-OneToMany-A" : "a", "X-OneToMany-B" : "b"]
        "GET"  | ["X-OneToMany-A" : "a", "X-OneToMany-B" : "b"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers one-to-many with removal"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-OneToManyRemoval-A")
        sentRequest.request.getHeaders().contains("X-OneToManyRemoval-B")
        sentRequest.request.getHeaders().contains("X-OneToManyRemoval-C")
        sentRequest.request.getHeaders().contains("X-OneToManyRemoval-D")

        and: "origin receives headers which should not be affected by the translation"
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-B").equalsIgnoreCase("b")

        and: "origin receives translated all header values"
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-C").contains("a")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-C").contains("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-C").contains("c")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-D").contains("a")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-D").contains("b")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-D").contains("c")

        and: "origin receives translated header values in order in which they were sent"
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-C") == reqHeaders.get("X-OneToManyRemoval-A")
        sentRequest.request.getHeaders().getFirstValue("X-OneToManyRemoval-D") == reqHeaders.get("X-OneToManyRemoval-A")


        where:
        method | reqHeaders
        "POST" | ["X-OneToManyRemoval-A" : "a,b,c", "X-OneToManyRemoval-B" : "b"]
        "GET"  | ["X-OneToManyRemoval-A" : "a,b,c", "X-OneToManyRemoval-B" : "b"]
        "POST" | ["X-OneToManyRemoval-A" : "a ,b,c,,", "X-OneToManyRemoval-B" : "b"]

    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers one-to-none"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers: reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        !sentRequest.request.getHeaders().contains("X-StripHeader-A")
        sentRequest.request.getHeaders().contains("X-StripHeader-B")
        sentRequest.request.getHeaders().getFirstValue("X-StripHeader-B").equalsIgnoreCase("b")

        where:
        method | reqHeaders
        "POST" | ["X-StripHeader-A" : "a", "X-StripHeader-B" : "b"]
        "GET"  | ["X-StripHeader-A" : "a", "X-StripHeader-B" : "b"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers many-to-many"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-ManyToMany-A")
        sentRequest.request.getHeaders().contains("X-ManyToMany-B")
        sentRequest.request.getHeaders().contains("X-ManyToMany-C")
        sentRequest.request.getHeaders().contains("X-ManyToMany-D")
        sentRequest.request.getHeaders().contains("X-ManyToMany-E")
        sentRequest.request.getHeaders().contains("X-ManyToMany-F")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-C").equalsIgnoreCase("c")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-D").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-E").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-ManyToMany-F").equalsIgnoreCase("c")

        where:
        method | reqHeaders
        "POST" | ["X-ManyToMany-A" : "a", "X-ManyToMany-B" : "b", "X-ManyToMany-C" : "c"]
        "GET"  | ["X-ManyToMany-A" : "a", "X-ManyToMany-B" : "b", "X-ManyToMany-C" : "c"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers many-to-one"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-ManyToOne-D")
        sentRequest.request.getHeaders().findAll("X-ManyToOne-D").contains("a")
        sentRequest.request.getHeaders().findAll("X-ManyToOne-D").contains("b")
        sentRequest.request.getHeaders().findAll("X-ManyToOne-D").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-ManyToOne-A" : "a", "X-ManyToOne-B" : "b", "X-ManyToOne-C" : "c"]
        "GET"  | ["X-ManyToOne-A" : "a", "X-ManyToOne-B" : "b", "X-ManyToOne-C" : "c"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers translating to existing header"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-ToExisting-A")
        sentRequest.request.getHeaders().contains("X-Header-Existing")
        sentRequest.request.getHeaders().getFirstValue("X-ToExisting-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("a")
        sentRequest.request.getHeaders().findAll("x-header-existing").contains("b")

        where:
        method | reqHeaders
        "POST" | ["X-ToExisting-A" : "a", "X-Header-Existing" : "b"]
        "GET"  | ["X-ToExisting-A" : "a", "X-Header-Existing" : "b"]
    }

    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating request headers with mixed case"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method: method, headers:reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "origin receives translated headers"
        sentRequest.request.getHeaders().contains("X-Mixedcase-A")
        sentRequest.request.getHeaders().contains("X-Mixedcase-B")
        sentRequest.request.getHeaders().contains("X-Mixedcase-C")
        sentRequest.request.getHeaders().contains("X-Mixedcase-D")
        sentRequest.request.getHeaders().contains("X-Mixedcase-E")
        sentRequest.request.getHeaders().getFirstValue("X-Mixedcase-A").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().getFirstValue("X-Mixedcase-B").equalsIgnoreCase("b")
        sentRequest.request.getHeaders().getFirstValue("X-Mixedcase-C").equalsIgnoreCase("c")
        sentRequest.request.getHeaders().getFirstValue("X-Mixedcase-D").equalsIgnoreCase("a")
        sentRequest.request.getHeaders().findAll("x-mixedcase-e").contains("b")
        sentRequest.request.getHeaders().findAll("x-mixedcase-e").contains("c")

        where:
        method | reqHeaders
        "POST" | ["X-Mixedcase-A" : "a", "X-Mixedcase-B" : "b", "X-Mixedcase-C" : "c"]
        "GET"  | ["X-Mixedcase-A" : "a", "X-Mixedcase-B" : "b", "X-Mixedcase-C" : "c"]
    }


    @Unroll("Request Verb: #method Headers: #reqHeaders")
    def "when translating CSL request headers"() {

        when: "client passes a request through repose with headers to be translated"
        def respFromOrigin = deproxy.makeRequest(url:(String) reposeEndpoint, method: method, headers: reqHeaders)
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
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }
}
