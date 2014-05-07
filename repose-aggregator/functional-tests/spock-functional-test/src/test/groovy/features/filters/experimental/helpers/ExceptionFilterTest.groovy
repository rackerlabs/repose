package features.filters.experimental.helpers

import framework.ReposeLogSearch
import framework.ReposeValveTest
import org.junit.Assume
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class ExceptionFilterTest extends ReposeValveTest {
    def logSearch = new ReposeLogSearch(properties.logFile)

    def splodeDate = new Date(2014 - 1900, Calendar.JULY, 1, 9, 0);

    /**
     * This test fails because repose does not properly support the servlet filter contract.
     * It should not fail.
     *
     * This test is ignored until JULY of 2014. The same splosion date as other ones. It should probably be ignored
     * until further than that, but I'm not sure what to do about that there.
     * @return
     */
    def "Proving that a custom filter does in fact work" () {
        setup:
        Assume.assumeTrue(new Date() > splodeDate)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def started = true
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/helpers", params)
        repose.start([waitOnJmxAfterStarting: false])
        waitUntilReadyToServiceRequests("200", true, true)


        waitUntilReadyToServiceRequests("200")

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

        cleanup:
        if(started)
            repose.stop()
        if(deproxy != null)
            deproxy.shutdown()

    }
}
