package features.core.startup
import framework.*
import org.rackspace.deproxy.Deproxy
import spock.lang.Ignore
import spock.lang.Specification

/**
 * This test was written to verify the startup behavior of Repose running in a container. It would leverage the
 * embedded container testing framework. Unfortunately, that framework does not currently support logging of Repose
 * startup events, and thus is not sufficient to test startup conditions.
 */

@Ignore
class ReposeStartupGlassfishTest extends Specification {
    static def reposeGlassfishEndpoint

    static int reposePort
    static int shutdownPort
    static String rootWar
    static Deproxy deproxy
    static ReposeLauncher repose
    static ReposeLogSearch reposeLogSearch
    static ReposeConfigurationProvider configProvider

    static TestProperties properties = new TestProperties()

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def logFile = properties.logFile

        rootWar = properties.getReposeRootWar()
        reposePort = properties.reposePort
        shutdownPort = properties.reposeShutdownPort

        configProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposeGlassfishEndpoint = "http://localhost:${reposePort}"

        params += ['reposePort': reposePort]

        configProvider.applyConfigs("features/core/systemprops", params)
        configProvider.applyConfigs("common", params)

        reposeLogSearch = new ReposeLogSearch(logFile)
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    def cleanupSpec() {
        repose?.stop()
        deproxy?.shutdown()
    }

    def "when Repose is started without the repose-cluster-id property, a message should be logged and Repose should stop"() {
        given:
        repose = new ReposeContainerLauncher(configProvider, properties.getGlassfishJar(), null, "node", rootWar, reposePort, shutdownPort)

        when:
        repose.start()
        sleep(10000)

        then:
        reposeLogSearch.searchByString("repose-cluster-id not provided -- Repose cannot start").size() == 1
    }
}
