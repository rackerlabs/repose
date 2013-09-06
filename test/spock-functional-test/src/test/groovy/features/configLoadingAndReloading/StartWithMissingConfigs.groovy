package features.configLoadingAndReloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.category.Slow
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithMissingConfigs extends Specification {

    int reposePort
    int stopPort
    int targetPort
    String url
    Properties properties
    def configDirectory
    ReposeConfigurationProvider reposeConfigProvider
    def logFile
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]
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

        // setup config provider
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())
        logFile = properties.getProperty("repose.log")
        configDirectory = properties.getProperty("repose.config.directory")
        def configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

    }

    @Unroll
    def "start with missing #componentLabel config"() {

        given:

        // set the common configs, but not the component-specific configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/configLoadingAndReloading/${componentLabel}-common",
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

