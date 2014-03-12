package framework

import org.apache.commons.io.FileUtils
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import spock.lang.Shared
import spock.lang.Specification

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import org.rackspace.deproxy.MessageChain

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

        properties = new TestProperties()

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
        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(properties)

        repose = new ReposeContainerLauncher(glassfishJar)
    }


    def configureReposeValve() {

        ReposeConfigurationProvider reposeConfigProvider = new ReposeConfigurationProvider(properties)

        repose = new ReposeValveLauncher(reposeConfigProvider, properties)
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose?.isUp())
            repose.stop()
    }

    def cleanLogDirectory() {
        FileUtils.deleteQuietly(new File(logFile))
    }

    def waitUntilReadyToServiceRequests(String responseCode = '200') {
        def clock = new SystemClock()
        def innerDeproxy = new Deproxy()
        MessageChain mc
        waitForCondition(clock, '35s', '1s', {
            try {
                mc = innerDeproxy.makeRequest([url: reposeEndpoint])
            } catch (Exception e) {}
            if (mc != null) {
                return mc.receivedResponse.code.equals(responseCode)
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

    def getConfigTemplates() {
        return properties.getConfigTemplates()
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
