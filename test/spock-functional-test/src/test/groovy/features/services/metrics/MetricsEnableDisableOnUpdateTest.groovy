package features.services.metrics

import framework.ReposeValveTest
import framework.category.Bug
import org.rackspace.gdeproxy.Deproxy
import org.junit.experimental.categories.Category

@Category(Bug.class)
class MetricsEnableDisableOnUpdateTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi.filters\":type=\"DestinationRouter\",scope=\""
    String RESPONSE_CODE_PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""

    String NAME_TARGET = "\",name=\"endpoint\""
    String NAME_2XX = "\",name=\"2XX\""
    String REPOSE_2XX = RESPONSE_CODE_PREFIX + "Repose" + NAME_2XX
    String ALL_ENDPOINTS_2XX = RESPONSE_CODE_PREFIX + "All Endpoints" + NAME_2XX

    String DESTINATION_ROUTER_TARGET = PREFIX + "destination-router" + NAME_TARGET
    def destRouterTarget = null
    def repose2xx = null
    def allEndpoints2xx = null

    def setupSpec() {

        repose.applyConfigs(
                "features/services/metrics/common")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def setup(){
        destRouterTarget = repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count")
        repose2xx = repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count")
        allEndpoints2xx = repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count")

        destRouterTarget = (destRouterTarget != null ? destRouterTarget :0)
        repose2xx = (repose2xx != null ? repose2xx :0)
        allEndpoints2xx = (repose2xx != null ? repose2xx :0)

    }

    def cleanupSpec(){

        repose.stop()
        deproxy.shutdown()
    }

    def "when metrics are enabled, reporting should occur"() {

        setup: "load the correct configuration file"
        repose.updateConfigs( "features/services/metrics/common",
                "features/services/metrics/metricsenabled" )
        when:
        deproxy.makeRequest(reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == (destRouterTarget + 1)
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == (repose2xx + 1)
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == (allEndpoints2xx + 1)
    }

    def "when metrics are disabled, reporting should not occur"() {

        setup: "load the correct configuration file"
        repose.updateConfigs( "features/services/metrics/metricsdisabled" )

        when:
        deproxy.makeRequest(reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == destRouterTarget
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == repose2xx
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == allEndpoints2xx
    }

    def "when 'enabled' is not specified, reporting should occur"() {

        setup: "load the correct configuration file"
        repose.updateConfigs( "features/services/metrics/common",
                "features/services/metrics/metricsenabled" )

        when:
        deproxy.makeRequest(reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == (destRouterTarget + 1)
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == (repose2xx + 1)
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == (allEndpoints2xx + 1)
    }

    def "when metrics config is missing, reporting should occur"() {

        setup: "only load the common configuration files"
        repose.updateConfigs( "features/services/metrics/common" )

        when:
        deproxy.makeRequest(reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == (destRouterTarget + 1)
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == (repose2xx + 1)
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == (allEndpoints2xx + 1)
    }

}
