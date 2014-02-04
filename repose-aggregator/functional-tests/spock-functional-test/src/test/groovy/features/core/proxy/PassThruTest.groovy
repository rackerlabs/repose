package features.core.proxy

import framework.ReposeValveTest
import framework.category.Bug
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
import org.junit.experimental.categories.Category

class PassThruTest extends ReposeValveTest {

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getTargetPort())
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
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

    @Unroll("Should pass path #requestpath")
    def "should pass all '/' characters to origin service"() {

        when: "client passes a request through repose with extra '/' characters"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: requestpath)
        def requestAtOriginService = messageChain.handlings[0].request

        then: "repose should preserve all '/' characters"
        requestAtOriginService.path.equals(requestpath)

        where:
        requestpath               | _
        "/something/////"         | _
        "/something//resource///" | _
    }

    @Unroll("Header: #weirdHeader should be passed")
    def "should pass all headers"() {


        when: "client passes a request through repose with unusual headers"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, headers: ["weirdheader": weirdHeader])
        def requestAtOriginService = messageChain.handlings[0].request

        then: "repose should pass all headers through"
        requestAtOriginService.headers["weirdheader"].equals(weirdHeader)

        where:
        weirdHeader                                                             | _
        "y"                                                                     | _
        "z,abc"                                                                 | _
        "Mozilla/5.0 \\(X11; FreeBSD amd64; rv:17.0\\) Gecko/17.0 Firefox/17.0" | _
        "a;b=c=1"                                                               | _
    }

    def "should rewrite host header"() {

        when: "client passes a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint)
        def requestAtOriginService = messageChain.handlings[0].request

        then: "repose should rewrite the host header"
        !requestAtOriginService.headers["host"].equals(messageChain.sentRequest.headers["host"])
    }

    @Category(Bug.class) // Defect D-11822
    @Unroll("Should not interfere with semicolons and equals signs: #name - #value")
    def "Should not interfere with semicolons and equals signs"() {

        //This test checks that Repose is not trying to look for qvalues in the
        // field values of headers that don't accept qvalues.

        // Substituting test values for the Host and Expect headers generally breaks
        // the protocol and confuses servers. Therefore, they are not included in
        // this test.

        // The Accept, Accept-Charset, Accept-Encoding, Accept-Language, and TE
        // headers accept qvalue parameters. They will be considered as-is, and are
        // not subject to the requirement.

        // The X-PP-User, X-PP-Groups, and X-Roles headers, while not defined by
        // RFC 2616, are well known to Repose and utilize qvalues. They are not
        // subject to the requirement.

        // The Connection, Keep-Alive, Proxy-Authenticate, Proxy-Authorization,
        // TE, Trailer, Transfer-Encoding, Upgrade, and trailers in the entity
        // body are "hop-by-hop" headers. These should not be naively forwarded
        // by a proxy, and are thus not subject to the requirement.

        // All other headers that are defined in RFC 2616 but not mentioned
        // above are tested, as well as some example extension headers not
        // defined by RFC 2616.

        given:
        def headers = [(name): value]

        when:
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName(name) == 1
        mc.handlings[0].request.headers[name] == value

        where:
        name                  | value
        'Accept-Ranges'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Age'                 | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Allow'               | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Authorization'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Cache-Control'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Encoding'    | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Language'    | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Length'      | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Location'    | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-MD5'         | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Range'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Content-Type'        | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Date'                | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'ETag'                | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Expires'             | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'From'                | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'If-Match'            | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'If-Modified-Since'   | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'If-None-Match'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'If-Range'            | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'If-Unmodified-Since' | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Last-Modified'       | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Location'            | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Max-Forwards'        | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Pragma'              | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Range'               | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Referer'             | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Retry-After'         | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Server'              | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'User-Agent'          | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Vary'                | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Via'                 | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'Warning'             | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'WWW-Authenticate'    | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'

        'Extension-Header'    | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
        'X-Extension-Header'  | 'something/1.0; q=0.5, another/0.8 ;q=0.8=0.3 ; a=b=c ; abc'
    }


}
