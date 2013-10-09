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
class MultiClusterMultiNodeTest extends Specification {

    int endpointPort1
    int endpointPort2
    Deproxy deproxy
    DeproxyEndpoint endpoint1
    DeproxyEndpoint endpoint2

    int port11
    int port12
    int port21
    int port22
    int stopPort
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    Map params = [:]

    def setup() {

        endpointPort1 = PortFinder.Singleton.getNextOpenPort()
        endpointPort2 = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        endpoint1 = deproxy.addEndpoint(endpointPort1)
        endpoint2 = deproxy.addEndpoint(endpointPort2)

        port11 = PortFinder.Singleton.getNextOpenPort()
        port12 = PortFinder.Singleton.getNextOpenPort()
        port21 = PortFinder.Singleton.getNextOpenPort()
        port22 = PortFinder.Singleton.getNextOpenPort()
        stopPort = PortFinder.Singleton.getNextOpenPort()

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        params = [
                'port11': port11,
                'port12': port12,
                'port21': port21,
                'port22': port22,
                'endpointPort1': endpointPort1,
                'endpointPort2': endpointPort2,
        ]
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/container-no-port",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/valveSelfConfigure/two-clusters-two-nodes-each",
                params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port11}",
                properties.getConfigDirectory(),
                port11,
                stopPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${port11}")
        repose.waitForNon500FromUrl("http://localhost:${port21}")
    }

    def "check that nodes are either available or not depending on hostname"() {

        def mc


        when: "send a request to node-1-1"
        mc = deproxy.makeRequest(url: "http://localhost:${port11}")

        then: "Repose forawrds the request"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == endpoint1


        when: "try to send a request to node-1-2"
        mc = deproxy.makeRequest(url: "http://localhost:${port12}")

        then:
        thrown(ConnectException)


        when: "send a request to node-2-1"
        mc = deproxy.makeRequest(url: "http://localhost:${port21}")

        then: "Repose forawrds the request"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == endpoint2


        when: "try to send a request to node-2-2"
        mc = deproxy.makeRequest(url: "http://localhost:${port22}")

        then:
        thrown(ConnectException)
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

