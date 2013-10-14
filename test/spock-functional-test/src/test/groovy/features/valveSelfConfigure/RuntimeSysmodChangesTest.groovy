package features.valveSelfConfigure

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.DeproxyEndpoint
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification
import org.junit.experimental.categories.Category

@Category(Slow.class)
class RuntimeSysmodChangesTest extends Specification {

    int endpointPort
    Deproxy deproxy
    DeproxyEndpoint endpoint

    int port1
    int port2
    int port3
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

        port1 = PortFinder.Singleton.getNextOpenPort()
        port2 = PortFinder.Singleton.getNextOpenPort()
        port3 = PortFinder.Singleton.getNextOpenPort()
        stopPort = PortFinder.Singleton.getNextOpenPort()

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        params = [
                'port1': port1,
                'port2': port2,
                'port3': port3,

                'proto': 'http',
                'endpointPort': endpointPort,
                'sysmod_port': port1,

        ]
        reposeConfigProvider.cleanConfigDirectory()

        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/container-no-port",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/single-node-with-proto",
                params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port1}",
                properties.getConfigDirectory(),
                port1,
                stopPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${port1}")
    }

    def "when making runtime changes to the system model, available nodes/ports/etc should change accordingly"() {

        def mc

        when: "Repose first starts up"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "Repose first starts up"
        deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "port 2 should not connect"
        thrown(ConnectException)

        when: "Repose first starts up"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two nodes"
        params = [
            'endpointPort': endpointPort,
            'node1host': 'localhost',
            'node2host': 'localhost',
            'node1port': port1,
            'node2port': port2,
        ]
        reposeConfigProvider.applyConfigsRuntime('features/valveSelfConfigure/two-nodes', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - one node on port 2"
        params = [
            'proto': 'http',
            'endpointPort': endpointPort,
            'sysmod_port': port2,
        ]
        reposeConfigProvider.applyConfigsRuntime('features/valveSelfConfigure/single-node-with-proto', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "port 1 should not connect"
        thrown(ConnectException)

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two of three nodes"
        params = [
            'endpointPort': endpointPort,
            'node1host': 'localhost',
            'node2host': 'localhost',
            'node3host': 'example.com',
            'node1port': port1,
            'node2port': port2,
            'node3port': port3,
        ]
        reposeConfigProvider.applyConfigsRuntime('features/valveSelfConfigure/three-nodes', params)
        sleep(sleep_duration)
        then:
        1 == 1


        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two of three nodes again, but different hostnames"
        params = [
            'endpointPort': endpointPort,
            'node1host': 'example.com',
            'node2host': 'localhost',
            'node3host': 'localhost',
            'node1port': port1,
            'node2port': port2,
            'node3port': port3,
        ]
        reposeConfigProvider.applyConfigsRuntime('features/valveSelfConfigure/three-nodes', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "port 1 should not connect"
        thrown(ConnectException)

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "node 3 should be available"
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