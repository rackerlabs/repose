package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class PassThruTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/proxy")
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

    def "should pass all '/' characters to origin service"(){

        when: "client passes a request through repose with extra '/' characters"
        def respFromOrigin = deproxy.makeRequest([url: reposeEndpoint + requestpath])
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then: "repose should preserve all '/' characters"
        sentRequest.getRequest().path.equals(requestpath)


        where:
        requestpath << ["/something/////", "/something//resource///"]


    }
}
