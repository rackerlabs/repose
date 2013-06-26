package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Ignore

class ResponseCodeJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""

    String NAME_2XX = "\",name=\"2XX\""
    String ALL_2XX = PREFIX + "All Endpoints" + NAME_2XX
    String REPOSE_2XX = PREFIX + "Repose" + NAME_2XX

    String NAME_5XX = "\",name=\"5XX\""
    String ALL_5XX = PREFIX + "All Endpoints" + NAME_5XX
    String REPOSE_5XX = PREFIX + "Repose" + NAME_5XX

    def handler5XX = { request -> return new Response(502, 'WIZARD FAIL') }

    def setup() {
        repose.applyConfigs("features/core/powerfilter/common")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        sleep(3000) //TODO: add a clean way to ensure deproxy has really shutdown all endpoints

        repose.stop()
    }

    // Greg/Dimitry: Is it expected that all2XX and repose2XX are equal?  It's not the sum of repose responses + origin service
    // responses?
    def "when sending requests, response code counters should be incremented"() {
        given:
        def responses = []

        when:
        responses.add(deproxy.makeRequest(reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(reposeEndpoint + "/endpoint"))
        responses.add(deproxy.makeRequest(reposeEndpoint + "/cluster"))

        then:
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == 3
        repose.jmx.getMBeanAttribute(ALL_2XX, "Count") == 3
        repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count") == null
        repose.jmx.getMBeanAttribute(ALL_5XX, "Count") == null

        responses.each { MessageChain mc ->
            mc.receivedResponse.code == 200
        }
    }

    def "when responses have 2XX and 5XX status codes, should increment 2XX and 5XX mbeans"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint", defaultHandler: handler5XX])
        deproxy.makeRequest(reposeEndpoint + "/cluster")

        then:
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == 1
        repose.jmx.getMBeanAttribute(ALL_2XX, "Count") == 1
        repose.jmx.getMBeanAttribute(REPOSE_5XX, "Count") == 1
        repose.jmx.getMBeanAttribute(ALL_5XX, "Count") == 1
    }

    /**
     *  TODO:
     *
     *  1) Need to verify counts for:
     *     - endpoint
     *     - cluster
     *     - All Endpoints
     *  2) Need to verify that Repose 5XX is sum of all non "All Endpoints" 5XX bean
     *  3) Need to verify that Repose 2XX is sum of all non "All Endpoints" 2XX bean
     */
    @Ignore
    def "when sending requests to service cluster, response codes should be recorded"() {

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.makeRequest(reposeEndpoint + "/endpoint");
        deproxy.makeRequest(reposeEndpoint + "/endpoint");
        deproxy.makeRequest(reposeEndpoint + "/cluster");

        def reposeCount = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")

        then:
        reposeCount == 4
    }
}
