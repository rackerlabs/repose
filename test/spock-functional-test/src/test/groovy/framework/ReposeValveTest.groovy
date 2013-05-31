package framework

import spock.lang.Shared
import spock.lang.Specification


abstract class ReposeValveTest extends Specification {

    @Shared def configDirectory
    @Shared def configSamples
    @Shared def ReposeConfigurationProvider reposeConfigProvider

    @Shared def ReposeLauncher reposeLauncher
    @Shared def ReposeClient reposeClient

    @Shared def Properties properties

    def setupSpec() {
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())
        configDirectory = properties.getProperty("repose.config.directory")
        configSamples = properties.getProperty("repose.config.samples")
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)
        reposeLauncher = new ReposeValveLauncher()
        reposeClient = new ReposeClient(properties.getProperty("repose.endpoint"))
    }

    def teardownSpec() {
        reposeLauncher.stop()
    }
}
