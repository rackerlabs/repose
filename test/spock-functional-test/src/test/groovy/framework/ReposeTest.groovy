package framework

import org.apache.commons.io.FileUtils
import org.linkedin.util.clock.SystemClock
import org.rackspace.gdeproxy.Deproxy
import spock.lang.Shared
import spock.lang.Specification

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import org.rackspace.gdeproxy.MessageChain

abstract class ReposeTest extends Specification {

    @Shared def configDirectory
    @Shared def logFile
    @Shared def configSamples
    @Shared def connFramework

    @Shared def ReposeLauncher repose

    @Shared def Deproxy deproxy

    @Shared def Properties properties

    @Shared def reposeEndpoint

    @Shared def ReposeLogSearch reposeLogSearch
    @Shared String reposeContainer

    def setupSpec() {
        properties = new Properties()
        properties.load(ClassLoader.getSystemResource("test.properties").openStream())

        configDirectory = properties.getProperty("repose.config.directory")
        configSamples = properties.getProperty("repose.config.samples")
        reposeEndpoint = properties.getProperty("repose.endpoint")
        logFile = properties.getProperty("repose.log")

        connFramework = "jersey"
        if (!reposeContainer)
            reposeContainer = properties.getProperty("repose.container")

        if (!reposeContainer) {
            reposeContainer = "valve"
        }

        switch (reposeContainer.toLowerCase()) {
            case "valve":
                configureReposeValve()
                break
            case "tomcat":
                throw new UnsupportedOperationException("Please implement me")
            case "glassfish":
                String glassfishJar = properties.getProperty("glassfish.jar")
                configureReposeGlassfish(glassfishJar)
                break
            default:
                throw new UnsupportedOperationException("Unknown container: " + reposeContainer)
        }
    }

    def configureReposeGlassfish(String glassfishJar) {
        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)

        repose = new ReposeGlassfishLauncher(glassfishJar)
    }

    def configureReposeValve() {
        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configSamples)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getProperty("repose.jar"),
                reposeEndpoint,
                configDirectory,
                properties.getProperty("repose.port").toInteger(),
                properties.getProperty("repose.shutdown.port").toInteger(),
                connFramework
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);
    }

    def teardownSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose.isUp())
            repose.stop()
    }

    def cleanLogDirectory() {
        FileUtils.deleteQuietly(new File(logFile))
    }

    def waitUntilReadyToServiceRequests() {
        def clock = new SystemClock()
        def innerDeproxy = new Deproxy()
        MessageChain mc
        waitForCondition(clock, '35s', '1s', {
            try {
            mc = innerDeproxy.makeRequest([url:reposeEndpoint])
            } catch (Exception e) {}
            if (mc != null) {
                return mc.receivedResponse.code.equals("200")
            } else {
                return false
            }
        })
    }
}
