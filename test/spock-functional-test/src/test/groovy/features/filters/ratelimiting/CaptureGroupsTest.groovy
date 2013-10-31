package features.filters.ratelimiting

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

class CaptureGroupsTest extends Specification {

    Deproxy deproxy
    int endpointPort

    int reposePort
    int stopPort
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    def setup() {

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
                "features/filters/ratelimiting/capturegroups",
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

    def "Requests to urls with capture groups should have separate buckets for different captured parts of the path"() {

        given:

        // the uri-regex is "/servers/(.+)/instances/(.+)"
        // if requests have different values for the captured part of the path,
        // they should be considered separately, and not affect each other

        def mc
        String url1 = "http://localhost:${reposePort}/servers/abc/instances/def"    // abc + def = abcdef
        String url2 = "http://localhost:${reposePort}/servers/abcde/instances/f"    // abcde + f = abcdef
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'group']


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


        when: "we make one request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should make it to the origin service"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a second request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1


        when: "we make a third request to the first url"
        mc = deproxy.makeRequest(url: url1, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0


        when: "we make a third request to the second url"
        mc = deproxy.makeRequest(url: url2, headers: headers)

        then: "it should be blocked"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }


    def cleanup() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
