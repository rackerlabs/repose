package features.filters.ratelimiting

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification

class NoCaptureGroupsTest extends Specification {

    static Deproxy deproxy
    static int endpointPort

    static int reposePort
    static int stopPort
    static TestProperties properties
    static ReposeConfigurationProvider reposeConfigProvider
    static ReposeValveLauncher repose

    def setupSpec() {

        endpointPort = PortFinder.Singleton.getNextOpenPort()
        deproxy = new Deproxy()
        deproxy.addEndpoint(endpointPort)

        reposePort = PortFinder.Singleton.getNextOpenPort()
        stopPort = PortFinder.Singleton.getNextOpenPort()

        properties = new TestProperties(ClassLoader.getSystemResource("test.properties").openStream())
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigSamples())

        def params = [
                'reposePort': reposePort,
                'endpointPort': endpointPort,
        ]
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigsRuntime(
                "common",
                params)
        reposeConfigProvider.applyConfigsRuntime(
                "features/filters/ratelimiting/nocapturegroups",
                params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${reposePort}",
                properties.getConfigDirectory(),
                reposePort,
                stopPort
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${reposePort}")
    }

    def "Urls that match the same pattern should go in the same bucket"() {

        given:

        def mc
        String url1 = "http://localhost:${reposePort}/objects/abc/things/123"
        String url2 = "http://localhost:${reposePort}/objects/def/things/456"
        def headers = ['X-PP-User': 'user5', 'X-PP-Groups': 'no-captures']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be block as well"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "Capture groups () should make no difference"() {

        given:

        def mc
        String url1 = "http://localhost:${reposePort}/servers/abc/instances/123"
        String url2 = "http://localhost:${reposePort}/servers/def/instances/456"
        def headers = ['X-PP-User': 'user5', 'X-PP-Groups': 'captures']


        when: "we make one request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a second request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)
        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0



        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)
        then: "it should be block as well"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }


    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
