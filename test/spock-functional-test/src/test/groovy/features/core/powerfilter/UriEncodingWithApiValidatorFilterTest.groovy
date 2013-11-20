package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class UriEncodingWithApiValidatorFilterTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/powerfilter/URIEncode/withAPIValidator")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "URI's with special character through API Validator filter"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent, method: "GET", headers: ["X-Roles": "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("404")
        messageChain.handlings.size() > 0
        messageChain.handlings.get(0).request.path.equals(URISent)



        where:
        URISent                | URItoriginService
        "/messages/+add-nodes" | "/messages/+add-nodes"


    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}