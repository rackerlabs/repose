package features.filters.versioning

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

/**
 * This test ensures that the versioning filter provides metrics via JMX,
 * counting how many requests it services and which endpoints it sends them to.
 *
 * http://wiki.openrepose.org/display/REPOSE/Repose+JMX+Metrics+Development
 *
 */

@Category(Slow.class)
class VersioningJMXTest extends Specification {

    String PREFIX = "\"repose-config-test-com.rackspace.papi.filters\":type=\"Versioning\",scope=\"versioning\""

    String VERSION_UNVERSIONED = "${PREFIX},name=\"Unversioned\""
    String VERSION_V1 = "${PREFIX},name=\"v1\""
    String VERSION_V2 = "${PREFIX},name=\"v2\""

    Deproxy deproxy

    TestProperties properties
    Map params
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    def setup() {

        properties = new TestProperties()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        deproxy.addEndpoint(properties.targetPort2)


        // configure and start repose

        reposeConfigProvider = new ReposeConfigurationProvider(properties.configDirectory, properties.configTemplates)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
        repose.enableDebug()

        params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)

    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/versioning/metrics", params)
        repose.start()
        sleep(30000)



        when:
        def mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        (repose.jmx.getMBeanAttribute(VERSION_V1, "Count") ?: 0) == 0
        (repose.jmx.getMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/v1/resource", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V1, "Count") == 1
        (repose.jmx.getMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/v2/resource", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V1, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V2, "Count") == 1

    }

    def cleanup() {
        if (repose && repose.isUp()) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }

}