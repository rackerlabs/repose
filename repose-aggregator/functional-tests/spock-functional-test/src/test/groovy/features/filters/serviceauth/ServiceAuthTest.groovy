package features.filters.serviceauth

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.TestProperties
import framework.TestUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.PortFinder
import org.rackspace.deproxy.Response
import spock.lang.Specification
import spock.lang.Unroll

class ServiceAuthTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint
    static authCredentials = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="

    def setupSpec() {

        def TestProperties properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        PortFinder pf = new PortFinder(properties.getDynamicPortBase())

        int originServicePort = pf.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = pf.getNextOpenPort()
        int shutdownPort = pf.getNextOpenPort()
        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configSamples = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configSamples)

        config.applyConfigsRuntime("features/filters/serviceauth",
                [
                        'repose_port': reposePort.toString(),
                        'target_port': originServicePort.toString(),
                        'repose.config.directory': configDirectory,
                        'repose.cluster.id': "repose1",
                        'repose.node.id': 'node1',
                        'target_hostname': 'localhost',
                ]
        )
        config.applyConfigsRuntime("common", ['project.build.directory': buildDirectory])


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

    @Unroll("When passing headers #headersPassed Repose should pass configured authorization credentials")
    def "should pass basic auth credentials"(){

        when: "Request is sent through Repose/Tomcat"
        TestUtils.waitUntilReadyToServiceRequests(tomcatEndpoint)
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: headersPassed)

        then: "Repose Should Send Authorization header with basic auth credentials"
        mc.handlings[0].request.getHeaders().contains("authorization")
        mc.handlings[0].request.getHeaders().getFirstValue("authorization").equals(authCredentials)

        where:
        headersPassed << [[:],["authorization":"basic blah"]]
    }

    @Unroll("When Origin Service returns a #originServiceResponse, Repose should return a 500")
    def "should return 500 when basic auth is rejected"(){

        given:
        def resp = { request -> return new Response(originServiceResponse) }

        when: "Request is sent through Repose/Tomcat"
        TestUtils.waitUntilReadyToServiceRequests(tomcatEndpoint)
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", defaultHandler: resp)

        then: "Repose Should Send Authorization header with basic auth credentials"
        mc.handlings[0].request.getHeaders().contains("authorization")
        mc.handlings[0].request.getHeaders().getFirstValue("authorization").equals(authCredentials)

        and: "Repose Should Return 500"
        mc.receivedResponse.code == "500"

        where:
        originServiceResponse << [403, 501]
    }

}
