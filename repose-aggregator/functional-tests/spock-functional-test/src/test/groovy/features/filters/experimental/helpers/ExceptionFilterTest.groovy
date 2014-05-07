package features.filters.experimental.helpers

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ExceptionFilterTest extends ReposeValveTest {
    def logSearch = new ReposeLogSearch(properties.logFile)

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/helpers", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("200", true, true)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "Proving that the test filter throws an exception" () {
        given:
        logSearch.cleanLog()
        
        when:
        MessageChain mc = null
        mc = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:reposeEndpoint + "/get",
                ])


        then:
        mc.receivedResponse.code == '500'
        logSearch.searchByString("java.lang.RuntimeException: This is just a test filter!  Don't use it in real life!").size() > 0
    }
}
