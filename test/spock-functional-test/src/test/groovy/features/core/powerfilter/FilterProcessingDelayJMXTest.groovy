package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

class FilterProcessingDelayJMXTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"FilterProcessingTime\",scope=\"Delay\""

    String API_VALIDATOR = PREFIX + ",name=\"api-validator\""
    String IP_IDENTITY = PREFIX + ",name=\"ip-identity\""

    def handler = {return new Response(200)}

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/powerfilter/multifilters")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "when a request is sent through Repose, should record per filter delay metrics"() {
        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ["X-Roles" : "role-1"],
                defaultHandler: handler)

        then:
        repose.jmx.getMBeanAttribute(IP_IDENTITY, "Count")  > 0
        repose.jmx.getMBeanAttribute(API_VALIDATOR, "Count")  > 0
        repose.jmx.getMBeanAttribute(IP_IDENTITY, "Mean") > 0
        repose.jmx.getMBeanAttribute(API_VALIDATOR, "Mean") > 0
    }
}
