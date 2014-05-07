package features.filters.experimental.helpers

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ExceptionFilterTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/helpers", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("500")

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "Proving that the test filter throws an exception" () {
        when:
        MessageChain mc = null
        mc = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:reposeEndpoint + "/v1/usertest1/servers/something",
                ])


        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.contains("<extra> Added by TestFilter, should also see the rest of the content </extra>")
    }
}
