package framework

import org.rackspace.gdeproxy.Deproxy
import spock.lang.Shared
import spock.lang.Specification


abstract class ReposeValveTest extends Specification {

    @Shared def configDirectory
    @Shared def configSamples

    @Shared def ReposeValveLauncher repose
    @Shared def Deproxy deproxy

    @Shared def Properties properties

    @Shared def reposeEndpoint

    @Shared def ReposeLogSearch reposeLogSearch

    def setupSpec() {
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())

        configDirectory = properties.getProperty("repose.config.directory")
        configSamples = properties.getProperty("repose.config.samples")
        reposeEndpoint = properties.getProperty("repose.endpoint")

        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                reposeEndpoint,
                configDirectory,
                properties.getProperty("repose.port").toInteger(),
                properties.getProperty("repose.shutdown.port").toInteger()
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getProperty("repose.log"));
    }

    def teardownSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose.isUp())
            repose.stop()
    }

}
