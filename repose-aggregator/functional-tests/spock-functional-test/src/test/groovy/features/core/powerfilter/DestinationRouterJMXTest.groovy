package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

@Category(Slow.class)
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
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when requests match destination router target URI, should increment DestinationRouter mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count")
        target = (target == null) ? 0 : target


        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/1"])
        deproxy.makeRequest([url: reposeEndpoint + "/cluster"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == (target + 1)
    }


    def "when requests match destination router target URI, should increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint2/2"])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/2"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == (target + 2)
    }

    def "when requests DO NOT match destination router target URI, should NOT increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == (target == 0 ? null : 0)
    }
}
