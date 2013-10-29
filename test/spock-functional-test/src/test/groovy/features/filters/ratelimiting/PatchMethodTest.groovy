package features.filters.ratelimiting

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.PortFinder
import spock.lang.Specification

class PatchMethodTest extends Specification {

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
                "features/filters/ratelimiting/oneNode",
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

    def "PATCH requests should be limited by limit-groups marked as 'PATCH'"() {

        given:
        def mc
        String url = "http://localhost:${reposePort}/patchmethod/resource"
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'patchmethod']


        when: "we make some PATCH requests"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make the final request that goes over the limit"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)

        then: "Repose should return an error and not forward the request to the origin service"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }

    def "PATCH requests should apply to limit-groups marked as 'ALL'"() {

        given:
        def mc
        String url ="http://localhost:${reposePort}/allmethods/resource"
        def headers = ['X-PP-User': 'user', 'X-PP-Groups': 'allmethods']

        when: "we make some requests with mixed methods"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make the final request that goes over the limit"
        mc = deproxy.makeRequest(method: "GET", url: url, headers: headers)

        then: "Repose should return an error and not forward the request to the origin service"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0
    }


    def "PATCH requests from one user shouldn't affect another user"() {

        given:
        def mc
        String url = "http://localhost:${reposePort}/patchmethod/resource"
        def headers1 = ['X-PP-User': 'user1', 'X-PP-Groups': 'patchmethod']
        def headers2 = ['X-PP-User': 'user2', 'X-PP-Groups': 'patchmethod']


        when: "we make some PATCH requests"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)

        then: "they should all come out ok"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when:
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1)
        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1




        when: "we make a separate request as another user"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers2)

        then: "Repose should let the request through"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }

    //@Ignore
    def "PATCH requests should allow all headers and body to pass through"() {

        given:
        def mc
        String url = "http://localhost:${reposePort}/patchmethod/resource"
        def headers1 = ['X-PP-User': 'user1', 'X-PP-Groups': 'patchmethod']

        when: "we make a PATCH request"
        mc = deproxy.makeRequest(method: "PATCH", url: url, headers: headers1, requestBody:"My Content Body")

        then: "the request headers and body should be passed to the origin service"
        mc.handlings.size() == 1
        mc.handlings[0].request.body == "My Content Body"
        mc.handlings[0].request.headers.contains("X-PP-User")
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
