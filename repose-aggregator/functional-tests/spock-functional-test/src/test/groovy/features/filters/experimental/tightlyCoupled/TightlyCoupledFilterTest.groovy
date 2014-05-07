package features.filters.experimental.tightlyCoupled

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class TightlyCoupledFilterTest extends ReposeValveTest {

    def splodeDate = new Date(2014 - 1900, Calendar.JULY, 1, 9, 0);

    /**
     * This test fails because repose does not properly support the servlet filter contract.
     * It should not fail.
     *
     * This test is ignored until JULY of 2014. The same splosion date as other ones. It should probably be ignored
     * until further than that, but I'm not sure what to do about that there.
     * @return
     */
    def "Proving that a custom filter (although tightly coupled) does in fact work" () {
        setup:

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/tightlycoupled", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def started = true
        repose.start([waitOnJmxAfterStarting: false])

        waitUntilReadyToServiceRequests("200")

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
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body.contains("<extra> Added by TestFilter, should also see the rest of the content </extra>")

        cleanup:
        if(started)
            repose.stop()
        if(deproxy != null)
            deproxy.shutdown()

    }
}
