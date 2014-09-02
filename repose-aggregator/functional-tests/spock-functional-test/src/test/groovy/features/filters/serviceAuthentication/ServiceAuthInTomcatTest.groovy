package features.filters.serviceAuthentication

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Specification
import spock.lang.Unroll

class ServiceAuthInTomcatTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint
    static authCredentials = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="

    def setupSpec() {

        def TestProperties properties = new TestProperties()

        int originServicePort = properties.targetPort
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = properties.reposePort

        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.configDirectory
        def rootWar = properties.reposeWar
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, properties.configTemplates)

        def params = properties.getDefaultTemplateParams()
        params += [
                'repose.cluster.id': "repose1",
                'repose.node.id': 'node1',
        ]

        config.applyConfigs("features/filters/serviceAuthentication", params)
        config.applyConfigs("common", params)

        repose = new ReposeContainerLauncher(config, properties.tomcatJar, "repose1", "node1", rootWar, reposePort)
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

    @Unroll("When passing headers #headersPassed Repose should pass configured authorization credentials")
    def "should pass basic auth credentials"() {

        when: "Request is sent through Repose/Tomcat"
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: headersPassed)

        then: "Repose Should Send Authorization header with basic auth credentials"
        mc.handlings[0].request.headers.contains("authorization")
        mc.handlings[0].request.headers.getFirstValue("authorization").equals(authCredentials)

        where:
        headersPassed                   | _
        [:]                             | _
        ["authorization": "basic blah"] | _
    }

    @Unroll("When Origin Service returns a #originServiceResponse, Repose should return a 500")
    def "should return 500 when basic auth is rejected"() {

        given:
        def resp = { request -> return new Response(originServiceResponse) }

        when: "Request is sent through Repose/Tomcat"
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", defaultHandler: resp)

        then: "Repose Should Send Authorization header with basic auth credentials"
        mc.handlings[0].request.getHeaders().contains("authorization")
        mc.handlings[0].request.getHeaders().getFirstValue("authorization").equals(authCredentials)

        and: "Repose Should Return 500"
        mc.receivedResponse.code == "500"

        where:
        originServiceResponse | _
        403                   | _
        501                   | _
    }

}
