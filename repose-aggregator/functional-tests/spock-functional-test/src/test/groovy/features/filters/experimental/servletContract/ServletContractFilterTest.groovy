package features.filters.experimental.servletContract

import framework.ReposeValveTest
import org.junit.Assume
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

class ServletContractFilterTest extends ReposeValveTest {

    def splodeDate = new Date(2015 - 1900, Calendar.JANUARY, 1, 9, 0)

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
        Assume.assumeTrue(new Date() > splodeDate)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/experimental/servletcontract", params)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def started = true
        repose.start([waitOnJmxAfterStarting: false])

        waitUntilReadyToServiceRequests("200", false, true)

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
        println(mc.receivedResponse.body)

        cleanup:
        if(started)
            repose.stop()
        if(deproxy != null)
            deproxy.shutdown()

    }
}
