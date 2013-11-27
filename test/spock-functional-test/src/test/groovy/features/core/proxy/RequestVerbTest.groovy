package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class RequestVerbTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/proxy")
        repose.connFramework = "apache"
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll("When sending a request through repose with a verb of #verb")
    def "sending a simple request with a specific verb"() {

        when: "Request is sent with a specific verb"
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, method: verb])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "Pass a request with the proper verb"
        ((Handling) sentRequest).request.method.equalsIgnoreCase(verb)

        where:
        verb << ["GET",
                "PUT",
                "POST",
                "PATCH",
                "OPTIONS",
                "DELETE",
                "TRACE",
                "HEAD"]


    }

}
