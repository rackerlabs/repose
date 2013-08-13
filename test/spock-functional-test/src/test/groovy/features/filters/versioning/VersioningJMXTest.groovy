package features.filters.uriNormalization

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

/**
 * This test ensures that the versioning filter provides metrics via JMX,
 * counting how many requests it services and which endpoints it ends them to.
 *
 * http://wiki.openrepose.org/display/REPOSE/Repose+JMX+Metrics+Development
 *
 */

class VersioningJMXTest extends Specification {

    String PREFIX = "\"repose-config-test-com.rackspace.papi.filters\":type=\"Versioning\",scope=\"versioning\""

    String VERSION_UNVERSIONED = "${PREFIX},name=\"Unversioned\""
    String VERSION_V1 = "${PREFIX},name=\"v1\""
    String VERSION_V2 = "${PREFIX},name=\"v2\""

    int reposePort
    int reposeStopPort
    int originServicePort1
    int originServicePort2
    String urlBase

    Deproxy deproxy

    Properties properties
    def logFile
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch

    def setup() {

        // get ports
        PortFinder pf = new PortFinder()

        reposePort = pf.getNextOpenPort()
        reposeStopPort = pf.getNextOpenPort()
        originServicePort1 = pf.getNextOpenPort()
        originServicePort2 = pf.getNextOpenPort()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort1)
        deproxy.addEndpoint(originServicePort2)


        // configure and start repose
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())

        def targetHostname = properties.getProperty("target.hostname")
        urlBase = "http://${targetHostname}:${reposePort}"
        logFile = properties.getProperty("repose.log")

        def configDirectory = properties.getProperty("repose.config.directory")
        def configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                urlBase,
                configDirectory,
                reposePort,
                reposeStopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);

        reposeConfigProvider.applyConfigsRuntime(
                "common",
                [   'reposePort': reposePort.toString(),
                    'targetPort1': originServicePort1.toString(),
                    'targetPort2': originServicePort2.toString()])

    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigsRuntime(
                "features/filters/versioning/metrics",
                [   'reposePort': reposePort.toString(),
                    'targetPort1': originServicePort1.toString(),
                    'targetPort2': originServicePort2.toString()])
        repose.start()



        when:
        def mc = deproxy.makeRequest(url: "${urlBase}", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        (repose.jmx.getMBeanAttribute(VERSION_V1, "Count") ?: 0) == 0
        (repose.jmx.getMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${urlBase}/v1/resource", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V1, "Count") == 1
        (repose.jmx.getMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${urlBase}/v2/resource", method: "GET")

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