package features.core.clusterdestination
import framework.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Specification

class DestinationClusterTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {

        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())

        int originServicePort1 = properties.targetPort
        int originServicePort2 = properties.targetPort2
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort1)
        deproxy.addEndpoint(originServicePort2)

        int reposePort = properties.getReposePort()

        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configSamples)

        def params = properties.getDefaultTemplateParams()
        params += [
                'repose_port': reposePort.toString(),
                'dst_port1': originServicePort1.toString(),
                'dst_port2': originServicePort2.toString(),
                'repose.config.directory': configDirectory,
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
                'target_hostname': 'localhost',
                'project.build.directory': buildDirectory
        ]

        config.applyConfigs("features/core/proxy/clusterdestination", params)
        config.applyConfigs("common", params)

        repose = new ReposeContainerLauncher(config, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort)
        repose.clusterId = "repose"
        repose.nodeId = "simple-node"
        repose.start()
        repose.waitForNon500FromUrl(tomcatEndpoint, 120)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    def "should switch routing between to nodes within destination cluster"(){

        when: "Requests are sent through Repose/Tomcat"
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: ['x-pp-user': 'usertest1'])
        MessageChain mc2 = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: ['x-pp-user': 'usertest1'])
        def sentRequest1 = ((MessageChain) mc).getHandlings()[0]
        def sentRequest2 = ((MessageChain) mc2).getHandlings()[0]

        then: "Responses should come back successfully"
        mc.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"

        and: "Repose should have routed to two different endpoints"
        sentRequest1.endpoint != sentRequest2.endpoint

    }
}
