package features.core.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

@Category(Slow.class)
class StartWithAPIValidatorVersion2AndUseSaxon extends Specification {

    int reposePort
    int stopPort
    String url
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch
    int targetPort
    def params = [:]
    Deproxy deproxy

    def setup() {
        TestProperties properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.stopPort = properties.reposeShutdownPort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // set initial config files

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        params = properties.getDefaultTemplateParams()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/common", params)
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/validator-common", params)
        reposeConfigProvider.applyConfigs("features/core/configLoadingAndReloading/validator-v2-use-saxon", params)

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
    }

    def "test_start_v2_use_saxon"() {

        expect:
        deproxy.makeRequest(url: url).receivedResponse.code == "503"
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

