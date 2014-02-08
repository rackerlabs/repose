package features.services.datastore

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.TestProperties
import framework.TestUtils
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

@Category(Slow.class)
class DistDatastoreServiceGlassfishBurstTest extends ReposeValveTest {

    static def reposeGlassfishEndpoint1
    static def reposeGlassfishEndpoint2
    static def datastoreGlassfishEndpoint1
    static def datastoreGlassfishEndpoint2

    static Deproxy deproxy

    static ReposeLauncher repose1
    static ReposeLauncher repose2

    static ReposeLogSearch reposeLogSearch1
    static ReposeLogSearch reposeLogSearch2

    static def params

    def setup() {

        def TestProperties properties = new TestProperties()
        def logFile = properties.logFile

        // get ports
        int originServicePort = properties.targetPort

        println("Deproxy: " + originServicePort)
        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        int reposePort1 = properties.reposePort
        int reposePort2 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort1 = PortFinder.Singleton.getNextOpenPort()
        int dataStorePort2 = PortFinder.Singleton.getNextOpenPort()
        int shutdownPort1 = properties.reposeShutdownPort
        int shutdownPort2 = PortFinder.Singleton.getNextOpenPort()

        println("repose1: ${reposePort1}")
        println("repose2: ${reposePort2}")
        println("datastore1: ${dataStorePort1}")
        println("datastore2: ${dataStorePort2}")

        // configure and start repose

        reposeGlassfishEndpoint1 = "http://localhost:${reposePort1}"
        reposeGlassfishEndpoint2 = "http://localhost:${reposePort2}"

        datastoreGlassfishEndpoint1 = "http://localhost:${dataStorePort1}"
        datastoreGlassfishEndpoint2 = "http://localhost:${dataStorePort2}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()

        params = properties.getDefaultTemplateParams()
        params += [
                'reposePort1': reposePort1,
                'reposePort2': reposePort2,
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
                'datastorePort1' : dataStorePort1,
                'datastorePort2' : dataStorePort2
        ]

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configTemplates)

        config1.applyConfigs("features/services/datastore/burst", params)
        config1.applyConfigs("common", params)

        repose1 = new ReposeContainerLauncher(config1, properties.getGlassfishJar(), "repose1", "node1", rootWar, reposePort1, shutdownPort1)
        reposeLogSearch1 = new ReposeLogSearch(logFile);

        repose1.start()
        repose1.waitForNon500FromUrl(reposeGlassfishEndpoint1, 120)
        repose1.waitForNon500FromUrl(datastoreGlassfishEndpoint1, 120)

        repose2 = new ReposeContainerLauncher(config1, properties.getGlassfishJar(), "repose1", "node2", rootWar, reposePort2, shutdownPort2)
        reposeLogSearch2 = new ReposeLogSearch(logFile);
        repose2.start()
        repose2.waitForNon500FromUrl(reposeGlassfishEndpoint2, 120)
        repose2.waitForNon500FromUrl(datastoreGlassfishEndpoint2, 120)
        TestUtils.waitUntilReadyToServiceRequests(reposeGlassfishEndpoint1, "401")
        TestUtils.waitUntilReadyToServiceRequests(reposeGlassfishEndpoint2, "401")

    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()

        if (repose1)
            repose1.stop()

        if (repose2)
            repose2.stop()

    }


    def "under heavy load should not go over specified rate limit"() {

        given:
        List<Thread> clientThreads = new ArrayList<Thread>()
        def totalSuccessfulCount = 0
        def totalFailedCount = 0
        List<String> requests = new ArrayList()
        def headers = ['X-PP-USER': '1']
        int rate_limiting_count = 10

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)
                    def messageChain = deproxy.makeRequest(url: (String) reposeGlassfishEndpoint1, method: "GET", headers: headers)
                    if(messageChain.receivedResponse.code.equals("200"))
                        totalSuccessfulCount = totalSuccessfulCount + 1
                    else
                        totalFailedCount = totalFailedCount + 1

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        println totalSuccessfulCount
        println totalFailedCount
        println requests.size()
        inRange(totalSuccessfulCount, rate_limiting_count)

        where:
        numClients | callsPerClient
        30         | 20
        10         | 50
        50         | 10

    }

    private void inRange(int totalSuccessfulCount, int rateLimitingCount){
        int minRange = Math.floor(rateLimitingCount - (rateLimitingCount * 0.03))
        int maxRange = Math.ceil(rateLimitingCount + (rateLimitingCount * 0.03))
        assert totalSuccessfulCount >= minRange
        assert totalSuccessfulCount <= maxRange
    }
}
