package features.filters.responsemessaging

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

class ResponseMessagingTest extends ReposeValveTest {

    def handler413 = { request -> return new Response(413, "test") }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/responsemessaging")
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


    def "when starting repose with the response messaging service, it should start"() {
        repose.isUp()
    }

    def "when endpoint returns a 413 response code, then repose should return the expected response body"() {
        when:
        sleep(10000)
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, defaultHandler: handler413])

        then:
        messageChain.receivedResponse.code == "413"
        messageChain.receivedResponse.body.contains("OverLimit Retry...")
    }
}
