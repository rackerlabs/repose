package features.core.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithMissingConfigs extends Specification {

    int reposePort
    int stopPort
    int targetPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]
    Deproxy deproxy

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.stopPort = properties.reposeShutdownPort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        params = properties.getDefaultTemplateParams()

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

    }

    @Unroll("start with missing #componentLabel config")
    def "start with missing #componentLabel config"() {

        given:

        // set the common configs, but not the component-specific configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/common", params)
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/${componentLabel}-common", params)

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort,
                stopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile());
        repose.start(killOthersBeforeStarting: false,
                     waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)



        expect: "if the file is missing then the default should produce 200's"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"

        where:
        componentLabel       | _
        "response-messaging" | _
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

