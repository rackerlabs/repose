package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain

class ContentLengthTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/proxy", "features/services/httpconnectionpool/chunkedfalse")
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    def "When set to send chunked encoding to false. Repose should not send requests chunked"() {

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "POST", requestBody: "blah"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == 0

        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == 1

        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length").equalsIgnoreCase("4")


    }
}
