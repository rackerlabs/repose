package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class NoHostHeaderRewriteTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/noHostRewrite", params)
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

    def "should not rewrite host header"(){

        when: "client passes a request through repose"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "repose should rewrite the host header"
        sentRequest.getRequest().getHeaders().getFirstValue("host").equals(respFromOrigin.getSentRequest().headers.getFirstValue("host"))


    }


}
