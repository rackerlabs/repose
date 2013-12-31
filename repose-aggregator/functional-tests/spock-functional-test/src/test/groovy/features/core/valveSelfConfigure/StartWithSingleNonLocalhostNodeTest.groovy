package features.core.valveSelfConfigure

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.DeproxyEndpoint
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification

@org.junit.experimental.categories.Category(Slow.class)
class StartWithSingleNonLocalhostNodeTest extends Specification {

    int targetPort
    Deproxy deproxy
    DeproxyEndpoint endpoint

    int port
    int stopPort
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    int sleep_duration = 35000

    def setup() {

        properties = new TestProperties()
        targetPort = properties.targetPort
        deproxy = new Deproxy()
        endpoint = deproxy.addEndpoint(targetPort)

        port = properties.reposePort
        stopPort = properties.reposeShutdownPort

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        def params = properties.getDefaultTemplateParams()
        params += [
                'host': 'example.com',
                'port': port,
        ]

        reposeConfigProvider.cleanConfigDirectory()

        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/one-node", params)
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

    def "when we start with a non-localhost hostname in a node and switch it to localhost"() {

        def mc

        when: "Repose first starts up"
        deproxy.makeRequest(url: "http://localhost:${port}")
        then: "it should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - change hostname to localhost"
        def params = properties.getDefaultTemplateParams()
        params += [
            'host': 'localhost',
            'port': port,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/one-node', params)
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
