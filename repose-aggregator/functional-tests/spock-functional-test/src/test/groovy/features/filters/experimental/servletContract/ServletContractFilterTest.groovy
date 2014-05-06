package features.filters.experimental.servletContract

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ServletContractFilterTest extends ReposeValveTest {

    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/servletfilter", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "Proving that a custom filter does in fact work" () {
        when:
        MessageChain mc = null
        mc = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:reposeEndpoint + "/v1/usertest1/servers/something",
                ])


        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body.contains("<extra> Added by TestFilter, should also see the rest of the content </extra>")
    }
}
