package features.valveSelfConfigure

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.DeproxyEndpoint
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

@org.junit.experimental.categories.Category(Slow.class)
class StartWithZeroNodesTest extends Specification {

    int endpointPort
    Deproxy deproxy
    DeproxyEndpoint endpoint

    int port
    int stopPort
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    Map params = [:]

    int sleep_duration = 35000

    def setup() {

        endpointPort = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        endpoint = deproxy.addEndpoint(endpointPort)

        port = PortFinder.Singleton.getNextOpenPort()
        stopPort = PortFinder.Singleton.getNextOpenPort()

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        params = [
                'host': 'example.com',
                'port': port,
                'endpointPort': endpointPort,
        ]

        reposeConfigProvider.cleanConfigDirectory()

        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/container-no-port",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/zero-nodes",
                params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port}",
                properties.getConfigDirectory(),
                port,
                stopPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        sleep(sleep_duration)
    }

    def "when we start with zero nodes in the system model, then switch to a system model with one  localhost node"() {

        def mc

        when: "Repose first starts up with zero nodes"
        deproxy.makeRequest(url: "http://localhost:${port}")
        then: "it should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - add a single localhost node"
        params = [
                'host': 'localhost',
                'port': port,
                'endpointPort': endpointPort,
        ]
        reposeConfigProvider.applyConfigsRuntime('features/valveSelfConfigure/one-node', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "Repose reloads the configs"
        mc = deproxy.makeRequest(url: "http://localhost:${port}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    def cleanup() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}