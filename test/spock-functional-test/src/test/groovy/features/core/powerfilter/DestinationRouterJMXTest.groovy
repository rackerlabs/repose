package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Response

/**
 * User: dimi5963
 * Date: 6/28/13
 * Time: 10:14 AM
 */
class DestinationRouterJMXTest extends ReposeValveTest{
    String PREFIX = "\"repose-node1-com.rackspace.papi.filters\":type=\"DestinationRouter\",scope=\""

    String NAME_TARGET = "\",name=\"endpoint\""
    String NAME_TARGET_ALL = "\",name=\"ACROSS ALL\""

    String DESTINATION_ROUTER_TARGET = PREFIX + "destination-router" + NAME_TARGET
    String DESTINATION_ROUTER_ALL = PREFIX + "destination-router" + NAME_TARGET_ALL

    def setup() {
        repose.applyConfigs("features/core/powerfilter/common")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when requests match destination router target URI, should increment DestinationRouter mbeans for specific endpoint"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/1"])
        deproxy.makeRequest([url: reposeEndpoint + "/cluster"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
    }


    def "when requests match destination router target URI, should increment DestinationRouter mbeans for all endpoints"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint2/2"])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/2"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == 2
    }

    def "when requests DO NOT match destination router target URI, should NOT increment DestinationRouter mbeans for all endpoints"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == null
    }
}
