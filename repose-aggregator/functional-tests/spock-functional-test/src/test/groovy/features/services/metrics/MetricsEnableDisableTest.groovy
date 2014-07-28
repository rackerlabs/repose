package features.services.metrics

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class MetricsEnableDisableTest extends ReposeValveTest {

    String PREFIX = "\"repose-node1-com.rackspace.papi.filters\":type=\"DestinationRouter\",scope=\""
    String RESPONSE_CODE_PREFIX = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""

    String NAME_TARGET = "\",name=\"endpoint\""
    String NAME_2XX = "\",name=\"2XX\""
    String REPOSE_2XX = RESPONSE_CODE_PREFIX + "Repose" + NAME_2XX
    String ALL_ENDPOINTS_2XX = RESPONSE_CODE_PREFIX + "All Endpoints" + NAME_2XX

    String DESTINATION_ROUTER_TARGET = PREFIX + "destination-router" + NAME_TARGET

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanup() {

        repose.stop()

    }

    def cleanupSpec(){

        deproxy.shutdown()
    }

    def "when metrics are enabled, reporting should occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsenabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == 1
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == 1
    }

    def "when metrics are disabled, reporting should not occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsdisabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == null
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == null
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == null
    }

    def "when 'enabled' is not specified, reporting should occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/notspecified", params)
        repose.start()

        when:
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
    }

    def "when metrics config is missing, reporting should occur"() {

        setup: "only load the common configuration files"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.start()

        when:
        deproxy.makeRequest(url:reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
    }

}
