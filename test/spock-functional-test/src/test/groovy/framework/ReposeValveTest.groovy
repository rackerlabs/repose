package framework

import org.apache.commons.io.FileUtils
import org.linkedin.util.clock.SystemClock
import org.rackspace.gdeproxy.Deproxy
import spock.lang.Shared
import spock.lang.Specification

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import org.rackspace.gdeproxy.MessageChain

abstract class ReposeValveTest extends Specification {

    @Shared
    def ReposeValveLauncher repose

    @Shared
    def Deproxy deproxy

    @Shared
    def TestProperties properties

    @Shared
    def ReposeLogSearch reposeLogSearch

    def setupSpec() {

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())

        switch (properties.getReposeContainer().toLowerCase()) {
            case "valve":
                configureReposeValve()
                break
            case "tomcat":
                throw new UnsupportedOperationException("Please implement me")
            case "multinode":
                String glassfishJar = properties.glassfishJar
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
                properties.getReposeJar(),
                properties.getReposeEndpoint(),
                properties.getConfigDirectory(),
                properties.getReposePort(),
                properties.getReposeShutdownPort(),
                properties.getConnFramework()
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
                mc = innerDeproxy.makeRequest([url: reposeEndpoint])
            } catch (Exception e) {}
            if (mc != null) {
                return mc.receivedResponse.code.equals("200")
            } else {
                return false
            }
        })
    }

    // Helper methods to minimize refactoring in all test classes
    def getReposeEndpoint() {
        return properties.getReposeEndpoint()
    }

    def getConnFramework() {
        return properties.getConnFramework()
    }

    def getConfigSamples() {
        return properties.getConfigSamples()
    }

    def getConfigDirectory() {
        return properties.getConfigDirectory()
    }

    def getLogFile() {
        return properties.getLogFile()
    }

    def getReposeContainer() {
        return properties.getReposeContainer()
    }


}
