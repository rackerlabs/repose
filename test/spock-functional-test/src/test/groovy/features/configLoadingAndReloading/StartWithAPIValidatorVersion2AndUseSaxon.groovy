package features.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.category.Slow
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
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
        PortFinder pf = new PortFinder()
        this.reposePort = pf.getNextOpenPort() as int
        this.stopPort = pf.getNextOpenPort() as int
        this.targetPort = pf.getNextOpenPort() as int
        this.url = "http://localhost:${this.reposePort}/"

        params = [
                'reposePort': this.reposePort,
                'targetHostname': 'localhost',
                'targetPort': targetPort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // set initial config files
        def properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())

        def logFile = properties.getProperty("repose.log")

        def configDirectory = properties.getProperty("repose.config.directory")
        def configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/validator-common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/validator-v2-use-saxon",
                params)

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                url,
                configDirectory,
                reposePort,
                stopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);
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

