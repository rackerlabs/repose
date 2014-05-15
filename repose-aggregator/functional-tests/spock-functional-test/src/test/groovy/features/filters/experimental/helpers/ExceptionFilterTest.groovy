package features.filters.experimental.helpers

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.junit.Assume
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class ExceptionFilterTest extends ReposeValveTest {
    def logSearch = new ReposeLogSearch(properties.logFile)

    def "Proving that the test filter throws an exception" () {
        given:
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def started = true
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/helpers", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("200", true, true)
        logSearch.cleanLog()

        when:
        MessageChain mc = null
        mc = deproxy.makeRequest(
                [
                        method: 'GET',
                        url:reposeEndpoint + "/get",
                        defaultHandler: {
                            new Response(200, null, null, "This should be the body")
                        }
                ])


        then:
        mc.receivedResponse.code == '500'
        logSearch.searchByString("java.lang.RuntimeException: This is just a test filter!  Don't use it in real life!").size() > 0

        cleanup:
        if(started)
            repose.stop()
        if(deproxy != null)
            deproxy.shutdown()

    }
}
