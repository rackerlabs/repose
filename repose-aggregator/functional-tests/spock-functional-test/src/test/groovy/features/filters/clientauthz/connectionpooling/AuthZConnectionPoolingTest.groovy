package features.filters.clientauthz.connectionpooling

import framework.mocks.MockIdentityService
import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.Handling
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: izrik
 *
 */
class AuthZConnectionPoolingTest extends Specification {

    int reposePort
    int reposeStopPort
    int originServicePort
    int identityServicePort
    String urlBase

    MockIdentityService identityService

    Deproxy deproxy
    Endpoint originEndpoint
    Endpoint identityEndpoint

    TestProperties properties
    def logFile
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    ReposeLogSearch reposeLogSearch

    def setup() {

        // get ports
        properties = new TestProperties()

        reposePort = properties.reposePort
        reposeStopPort = properties.reposeShutdownPort
        originServicePort = properties.targetPort
        identityServicePort = properties.identityPort

        identityService = new MockIdentityService(identityServicePort, originServicePort)

        // start deproxy
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(originServicePort)
        identityEndpoint = deproxy.addEndpoint(identityServicePort,
                "identity", "localhost", identityService.handler)


        // configure and start repose

        def targetHostname = properties.targetHostname
        urlBase = "http://${targetHostname}:${reposePort}"
        logFile = properties.logFile

        def configDirectory = properties.configDirectory
        def configTemplates = properties.configTemplates
        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                urlBase,
                configDirectory,
                reposePort,
                reposeStopPort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(logFile);

        def params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/filters/clientauthz/connectionpooling", params)
        repose.start()
    }

    def "when a client makes requests, Repose should re-use the connection to the Identity service"() {

        setup:
        def url = "${urlBase}/servers/tenantid/resource"


        when: "making two requests to Repose"
        def mc1 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token1'])
        def mc2 = deproxy.makeRequest(url: url, headers: ['X-Auth-Token': 'token2'])
        // collect all of the handlings that make it to the identity endpoint into one list
        def allOrphanedHandlings = mc1.orphanedHandlings + mc2.orphanedHandlings
        List<Handling> identityHandlings = allOrphanedHandlings.findAll { it.endpoint == identityEndpoint }
        def commons = allOrphanedHandlings.intersect(identityHandlings)
        def diff = allOrphanedHandlings.plus(identityHandlings)
        diff.removeAll(commons)


        then: "the connections for Repose's request to Identity should have the same id"

        mc1.orphanedHandlings.size() > 0
        mc2.orphanedHandlings.size() > 0
        identityHandlings.size() > 0
        // there should be no requests to auth with a different connection id
        diff.size() == 0
    }

    def cleanup() {

        if (repose && repose.isUp()) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
