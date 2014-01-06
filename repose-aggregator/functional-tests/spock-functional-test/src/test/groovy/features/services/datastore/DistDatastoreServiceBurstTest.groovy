package features.services.datastore

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.TestUtils
import framework.category.Slow
import framework.category.Bug
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification

@Category(Slow.class)
class DistDatastoreServiceBurstTest extends Specification {

    static def reposeEndpoint1
    static def datastoreEndpoint1

    static Deproxy deproxy

    static ReposeLauncher repose1

    def setupSpec() {

        def logFile
        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        // get ports
        PortFinder pf = new PortFinder(properties.getDynamicPortBase())

        int originServicePort = pf.getNextOpenPort()

        println("Deproxy: " + originServicePort)
        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)


        int reposePort1 = pf.getNextOpenPort()
        int dataStorePort1 = pf.getNextOpenPort()
        int shutdownPort1 = pf.getNextOpenPort()

        // configure and start repose

        reposeEndpoint1 = "http://localhost:${reposePort1}"

        datastoreEndpoint1 = "http://localhost:${dataStorePort1}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def buildDirectory = properties.getReposeHome() + "/.."

        ReposeConfigurationProvider config1 = new ReposeConfigurationProvider(configDirectory, configSamples)
        config1.applyConfigsRuntime("features/services/datastore/burst",
                [
                        'repose_port1': reposePort1.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'target_hostname': 'localhost',
                        'datastore_port1' : dataStorePort1
                ]
        )

        config1.applyConfigsRuntime("common", ['project.build.directory':buildDirectory])

        repose1 = new ReposeValveLauncher(config1, properties.getReposeJar(), reposeEndpoint1, configDirectory, reposePort1, shutdownPort1)
        repose1.start(["killOthersBeforeStarting":false, "waitOnJmxAfterStarting": false])
        TestUtils.waitUntilReadyToServiceRequests(reposeEndpoint1,"401")

    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose1)
            repose1.stop()

    }

    @Category(Bug.class)
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
                    def messageChain = deproxy.makeRequest(url: (String) reposeEndpoint1, method: "GET", headers: headers)

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
        assert totalSuccessfulCount == rate_limiting_count

        where:
        numClients | callsPerClient
        30        | 20

    }
}
