package features.core.proxy
import framework.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification

class TomcatProxyTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {

        def TestProperties properties = new TestProperties()

        int originServicePort = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = PortFinder.Singleton.getNextOpenPort()
        int shutdownPort = PortFinder.Singleton.getNextOpenPort()
        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configSamples)

        config.applyConfigs("features/core/proxy",
                [
                        'reposePort': reposePort.toString(),
                        'targetPort': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node1',
                        'targetHostname': 'localhost',
                ]
        )
        config.applyConfigs("common", ['project.build.directory': buildDirectory])


        repose = new ReposeContainerLauncher(config, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort, shutdownPort)
        repose.clusterId = "repose"
        repose.nodeId = "simple-node"
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    def "Should Pass Requests through repose"() {

        when: "Request is sent through Repose/Tomcat"
        TestUtils.waitUntilReadyToServiceRequests(tomcatEndpoint)
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: ['x-trace-request': 'true', 'x-pp-user': 'usertest1'])

        then: "Repose Should Forward Request"
        mc.handlings[0].request.getHeaders().contains("x-pp-user")

        and: "Repose Should Forward Response"
        mc.receivedResponse.code == "200"
    }
}
