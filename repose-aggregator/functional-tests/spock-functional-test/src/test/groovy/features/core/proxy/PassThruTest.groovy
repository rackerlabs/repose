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
        requestAtOriginService.headers.getFirstValue("weirdheader").equals(weirdHeader)

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

        then: "repose should not rewrite the host header"
        !requestAtOriginService.headers.getFirstValue("host").equals(messageChain.sentRequest.headers.getFirstValue("host"))


    }

}
