package framework

import deproxy.GDeproxy
import spock.lang.Shared
import spock.lang.Specification


abstract class ReposeValveTest extends Specification {

    @Shared def configDirectory
    @Shared def configSamples

    @Shared def ReposeValveLauncher repose
    @Shared def GDeproxy deproxy

    @Shared def Properties properties

    def setupSpec() {
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())
        configDirectory = properties.getProperty("repose.config.directory")
        configSamples = properties.getProperty("repose.config.samples")
        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeValveLauncher(reposeConfigProvider)
        repose.configDir = configDirectory
        repose.jmxPort = properties.getProperty("repose.jmxport")
        repose.shutdownPort = properties.getProperty("repose.shutdown.port")
        repose.reposeEndpoint = properties.getProperty("repose.endpoint")

        deproxy = new GDeproxy(properties.getProperty("repose.endpoint"))
    }

    def teardownSpec() {
        repose.stop()
    }

    abstract def foo()

}
