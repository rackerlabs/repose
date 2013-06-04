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

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                properties.getProperty("repose.endpoint"),
                configDirectory,
                properties.getProperty("repose.shutdown.port").toInteger(),
                properties.getProperty("repose.jmx.url"),
                properties.getProperty("repose.jmx.port").toInteger()
        )

        deproxy = new GDeproxy(properties.getProperty("repose.endpoint"))
    }

    def teardownSpec() {
        repose.stop()
    }

}
