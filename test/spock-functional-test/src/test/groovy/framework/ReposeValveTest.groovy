package framework

import spock.lang.Shared
import spock.lang.Specification


abstract class ReposeValveTest extends Specification {

    @Shared def configDirectory
    @Shared def configSamples
    @Shared def ConfigHelper configHelper

    @Shared def ReposeLauncher reposeLauncher
    @Shared def ReposeClient reposeClient

    @Shared def Properties properties

    def setupSpec() {
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())
        configDirectory = properties.getProperty("repose.config.directory")
        configSamples = properties.getProperty("repose.config.samples")
        configHelper = new ConfigHelper(configDirectory, configSamples)
        reposeLauncher = new ValveLauncher()
        reposeClient = new ReposeClient(properties.getProperty("repose.endpoint"))
    }

    def teardownSpec() {
        reposeLauncher.stop()
    }
}
