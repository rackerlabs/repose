package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class PassThruTest extends ReposeValveTest {

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getTargetPort())
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
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint, path: requestpath])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "repose should preserve all '/' characters"
        sentRequest.getRequest().path.equals(requestpath)


        where:
        requestpath << ["/something/////", "/something//resource///"]


    }

    @Unroll("Header: #weirdHeader should be passed")
    def "should pass all headers"() {


        when: "client passes a request through repose with unusual headers"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint, headers: ["weirdheader" : weirdHeader]])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "repose should pass all headers through"
        sentRequest.getRequest().getHeaders().getFirstValue("weirdheader").equals(weirdHeader)

        where:
        weirdHeader << [
                "y"
                , "z,abc"
                , "Mozilla/5.0 \\(X11; FreeBSD amd64; rv:17.0\\) Gecko/17.0 Firefox/17.0"
                , "a;b=c=1"
        ]
    }

    def "should rewrite host header"(){

        when: "client passes a request through repose"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "repose should not rewrite the host header"
        !sentRequest.getRequest().getHeaders().getFirstValue("host").equals(respFromOrigin.getSentRequest().headers.getFirstValue("host"))


    }

}
