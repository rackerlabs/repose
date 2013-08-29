package features.services.datastore
import framework.*
import org.linkedin.util.clock.SystemClock
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.PortFinder
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

/**
 * Test the Distributed Datastore Service in 2 multinode containers
 */
class DistDatastoreServiceGlassfishTest extends Specification {

    static def reposeGlassfishEndpoint1
    static def reposeGlassfishEndpoint2

    static Deproxy deproxy

    static ReposeLauncher repose1
    static ReposeLauncher repose2

    static ReposeLogSearch reposeLogSearch1
    static ReposeLogSearch reposeLogSearch2

    def setupSpec() {

        def logFile

        // get ports
        PortFinder pf = new PortFinder()

        int originServicePort = pf.getNextOpenPort()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort1 = pf.getNextOpenPort()
        int reposePort2 = pf.getNextOpenPort()

        // configure and start repose
        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())

        reposeGlassfishEndpoint1 = "http://localhost:${reposePort1}"
        reposeGlassfishEndpoint2 = "http://localhost:${reposePort2}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getConfigSamples()
        def rootWar = properties.getReposeRootWar()

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configSamples)
        config1.applyConfigsRuntime("common",
                [
                        'repose_port': reposePort1.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node1'
                ]
        )

        repose1 = new ReposeGlassfishLauncher(config1, properties.getGlassfishJar(), "repose1", "node1", rootWar, reposePort1)
        reposeLogSearch1 = new ReposeLogSearch(logFile);
        repose1.applyConfigs("features/services/datastore/multinode/node1")
        repose1.start()
        waitUntilReadyToServiceRequests(reposeGlassfishEndpoint1)

        configDirectory = configDirectory + "/node2"
        ReposeConfigurationProvider config2 = new ReposeConfigurationProvider(configDirectory, configSamples)
        config2.applyConfigsRuntime("common",
                [
                        'repose_port': reposePort2.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node2'

                ])
        repose2 = new ReposeGlassfishLauncher(config2, properties.getGlassfishJar(), "repose2", "node2", rootWar, reposePort2)
        reposeLogSearch2 = new ReposeLogSearch(logFile);
        repose2.applyConfigs("features/services/datastore/multinode/node2")
        repose2.start()
        waitUntilReadyToServiceRequests(reposeGlassfishEndpoint2)

    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose1)
            repose1.stop()

        if (repose2)
            repose2.stop()

    }

    def "when configured with DD service on Glassfish, repose should start and successfully execute calls"() {

        when:
        MessageChain mc1 = deproxy.makeRequest([url: reposeGlassfishEndpoint1 + "/cluster", headers: ['x-trace-request': 'true']])
        MessageChain mc2 = deproxy.makeRequest([url: reposeGlassfishEndpoint2 + "/cluster", headers: ['x-trace-request': 'true']])

        then:
        mc1.receivedResponse.code == '200'
        mc1.handlings.size() == 1

        mc2.receivedResponse.code == '200'
        mc2.handlings.size() == 1
    }

    def "when configured with at least 2 nodes, limits are shared and no 'damaged node' errors are recorded"() {
        given:
        def user = UUID.randomUUID().toString();

        when:
        //rate limiting is set to 3 an hour
        for (int i = 0; i < 3; i++) {
            MessageChain mc = deproxy.makeRequest(reposeGlassfishEndpoint1 + "/test", 'GET', ['X-PP-USER': user])
            if (mc.receivedResponse.code == 200) {
                throw new SpockAssertionError("Expected 200 response from repose")
            }
        }

        //this call should rate limit when calling the second node
        MessageChain mc = deproxy.makeRequest(reposeGlassfishEndpoint2 + "/test", 'GET', ['X-PP-USER': user])

        then:
        mc.receivedResponse.code == 413

        def List<String> logMatches = reposeLogSearch.searchByString("damaged node");
        logMatches.size() == 0
    }

    def waitUntilReadyToServiceRequests(String reposeEndpoint) {
        def clock = new SystemClock()
        def innerDeproxy = new Deproxy()
        MessageChain mc
        waitForCondition(clock, '60s', '1s', {
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
}
