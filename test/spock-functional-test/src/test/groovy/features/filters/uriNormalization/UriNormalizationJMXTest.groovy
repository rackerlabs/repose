package features.filters.uriNormalization

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

@Category(Slow.class)
class UriNormalizationJMXTest extends Specification {

    String PREFIX = "\"repose-config-test-com.rackspace.papi.filters\":type=\"UriNormalization\",scope=\"uri-normalization\""

    String URI_NORMALIZATION_ROOT_GET = "${PREFIX},name=\".\\*_GET\""
    String URI_NORMALIZATION_ROOT_POST = "${PREFIX},name=\".\\*_POST\""
    String URI_NORMALIZATION_RESOURCE_GET = "${PREFIX},name=\"/resource/.\\*_GET\""
    String URI_NORMALIZATION_RESOURCE_POST = "${PREFIX},name=\"/resource/.\\*_POST\""
    String URI_NORMALIZATION_SERVERS_GET = "${PREFIX},name=\"/servers/.\\*_GET\""
    String URI_NORMALIZATION_SERVERS_POST = "${PREFIX},name=\"/servers/.\\*_POST\""
    String URI_NORMALIZATION_SECONDARY_PATH_GET = "${PREFIX},name=\"/secondary/path/.\\*_GET\""
    String URI_NORMALIZATION_TERTIARY_PATH_GET = "${PREFIX},name=\"/tertiary/path/.\\*_GET\""
    String URI_NORMALIZATION_ACROSS_ALL = "${PREFIX},name=\"ACROSS ALL\""

    int reposePort
    int reposeStopPort
    int originServicePort
    String urlBase

    Deproxy deproxy

    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    def setup() {

        // get ports
        PortFinder pf = new PortFinder()

        reposePort = pf.getNextOpenPort()
        reposeStopPort = pf.getNextOpenPort()
        originServicePort = pf.getNextOpenPort()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        // configure and start repose
        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())

        def targetHostname = properties.getTargetHostname()
        urlBase = "http://${targetHostname}:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                urlBase,
                properties.getConfigDirectory(),
                reposePort,
                reposeStopPort
        )
        repose.enableDebug()

        reposeConfigProvider.applyConfigsRuntime(
                "common",
                [   'reposePort': reposePort.toString(),
                    'targetPort': originServicePort.toString()])

    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigsRuntime(
                "features/filters/uriNormalization/metrics/single",
                [   'reposePort': reposePort.toString(),
                    'targetPort': originServicePort.toString()])
        repose.start()
        sleep(30000)



        when:
        def mc = deproxy.makeRequest(url: "${urlBase}?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 2

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_RESOURCE_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 3

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_RESOURCE_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 4

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SERVERS_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 5

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SERVERS_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 6

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243?a=1", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 7

    }

    def "when multiple filter instances are configured, each should add to the count"() {

        given:
        reposeConfigProvider.applyConfigsRuntime(
                "features/filters/uriNormalization/metrics/multiple",
                [   'reposePort': reposePort.toString(),
                    'targetPort': originServicePort.toString()])
        repose.start()
        sleep(30000)



        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: "${urlBase}?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 1
        (repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") ?: 0) == 0
        (repose.jmx.getMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 1


        when: "client makes a request that matches two filters' uri-regex attributes (1 & 2)"
        mc = deproxy.makeRequest(url: "${urlBase}/secondary/path/asdf?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        (repose.jmx.getMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 3


        when: "client makes a request that matches two filters' uri-regex attributes (1 & 3)"
        mc = deproxy.makeRequest(url: "${urlBase}/tertiary/path/asdf?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ACROSS_ALL, "Count") == 5

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