package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class RequestBodyForwardingTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/forwarding", params)
        repose.start()
    }

    def "when GET request with body, Deproxy should forward body"() {
        given:
        String testBody = "some test body"

        when:
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.targetPort}", requestBody: testBody)

        then:
        mc.getSentRequest().getBody().toString().equals(testBody)
        mc.handlings.get(0).getRequest().getBody().toString().equals(testBody)
    }

    def "when GET request with body, Repose should drop body"() {
        given:
        String testBody = "some test body"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/get", requestBody: testBody)

        then:
        mc.getSentRequest().getBody().toString().equals(testBody)
        mc.handlings.get(0).getRequest().getBody().toString().equals("")
    }
}
