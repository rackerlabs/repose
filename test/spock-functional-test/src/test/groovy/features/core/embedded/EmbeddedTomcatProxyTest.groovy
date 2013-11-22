package features.core.embedded

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.TestProperties
import org.linkedin.util.clock.SystemClock
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

class EmbeddedTomcatProxyTest {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {

        PortFinder pf = new PortFinder()

        int originServicePort = pf.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = pf.getNextOpenPort()
        int shutdownPort = pf.getNextOpenPort()
        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configSamples)

        config.applyConfigsRuntime("common", ['project.build.directory': buildDirectory])
        config.applyConfigsRuntime("features/filters/ipidentity", ['project.build.directory': buildDirectory])

        config.applyConfigsRuntime("features/core/embedded",
                [
                        'repose_port': reposePort.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node1',
                        'target_hostname': 'localhost',
                ]
        )


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
        waitUntilReadyToServiceRequests(tomcatEndpoint)
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: ['x-trace-request': 'true', 'x-pp-user': 'usertest1'])

        then: "Repose Should Forward Request"
        mc.handlings[0].request.getHeaders().contains("x-pp-user")

        and: "Repose Should Forward Response"
        mc.receivedResponse.code == "200"
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
                if (mc.receivedResponse.code.equalsIgnoreCase("200")) {
                    return true
                }
            } else {
                return false
            }
        })
    }

}
