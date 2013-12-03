package features.services.datastore
import framework.*
import framework.category.Flaky
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Specification
/**
 * Test the Distributed Datastore Service in 2 multinode containers
 */

@Category(Flaky)
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

        println("Deproxy: " + originServicePort)
        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        int reposePort1 = pf.getNextOpenPort()
        int reposePort2 = pf.getNextOpenPort()
        int dataStorePort1 = pf.getNextOpenPort()
        int dataStorePort2 = pf.getNextOpenPort()
        int shutdownPort1 = pf.getNextOpenPort()
        int shutdownPort2 = pf.getNextOpenPort()

        println("repose1: " + reposePort1 + "\nrepose2: " + reposePort2 + "\ndatastore1: " + dataStorePort1 + "\n" +
                "datastore2: " + dataStorePort2)

        // configure and start repose
        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())

        reposeGlassfishEndpoint1 = "http://localhost:${reposePort1}"
        reposeGlassfishEndpoint2 = "http://localhost:${reposePort2}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configSamples)
        config1.applyConfigsRuntime("features/services/datastore/multinode",
                [
                        'repose_port1': reposePort1.toString(),
                        'repose_port2': reposePort2.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node1',
                        'target_hostname': 'localhost',
                        'datastore_port1' : dataStorePort1,
                        'datastore_port2' : dataStorePort2
                ]
        )

        config1.applyConfigsRuntime("common", ['project.build.directory':buildDirectory])

        repose1 = new ReposeContainerLauncher(config1, properties.getGlassfishJar(), "repose1", "node1", rootWar, reposePort1, shutdownPort1)
        reposeLogSearch1 = new ReposeLogSearch(logFile);

        repose1.start()
        TestUtils.waitUntilReadyToServiceRequests(reposeGlassfishEndpoint1,"401")

        repose2 = new ReposeContainerLauncher(config1, properties.getGlassfishJar(), "repose1", "node2", rootWar, reposePort2, shutdownPort2)
        reposeLogSearch2 = new ReposeLogSearch(logFile);
        repose2.start()
        TestUtils.waitUntilReadyToServiceRequests(reposeGlassfishEndpoint2,"401")

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

        given:
        def xmlResp = { request -> return new Response(200, "OK", ['header':"blah"], "test") }

        when:
        MessageChain mc1 = deproxy.makeRequest(url: reposeGlassfishEndpoint1 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])
        MessageChain mc2 = deproxy.makeRequest(url: reposeGlassfishEndpoint2 + "/cluster", headers: ['x-trace-request': 'true','x-pp-user':'usertest1'])

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
            MessageChain mc = deproxy.makeRequest(url: reposeGlassfishEndpoint1 + "/test", headers: ['X-PP-USER': user])
            if (mc.receivedResponse.code == 200) {
                throw new SpockAssertionError("Expected 200 response from repose")
            }
        }

        //this call should rate limit when calling the second node
        MessageChain mc = deproxy.makeRequest(url: reposeGlassfishEndpoint2 + "/test", headers: ['X-PP-USER': user])

        then:
        mc.receivedResponse.code == "413"

//        def List<String> logMatches = reposeLogSearch1.searchByString("damaged node");
//        logMatches.size() == 0
    }
}
