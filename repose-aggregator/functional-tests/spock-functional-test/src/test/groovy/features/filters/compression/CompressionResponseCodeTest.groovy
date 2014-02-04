package features.filters.compression

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder

class CompressionResponseCodeTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(PortFinder.Singleton.getNextOpenPort())

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/compression", params)
        repose.start()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "when decompression fails with EOF Exception, return 400"() {
        when:
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint, method:"POST",
                headers:["Content-Encoding":"gzip"],
                requestBody:"")

        then:
        mc.handlings.size()      == 0
        mc.receivedResponse.code == "400"
    }
}
