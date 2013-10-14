package features.filters.ratelimiting

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

class PatchMethodTest extends Specification {

    Deproxy deproxy
    int endpointPort

    int reposePort
    int stopPort
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    def setup() {

        endpointPort = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(endpointPort)

        reposePort = PortFinder.Singleton.getNextOpenPort()
        stopPort = PortFinder.Singleton.getNextOpenPort()

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        def params = [
                'reposePort': reposePort,
                'endpointPort': endpointPort,
        ]
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/filters/ratelimiting/oneNode",
                params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${reposePort}",
                properties.getConfigDirectory(),
                reposePort,
                stopPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${reposePort}")
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
