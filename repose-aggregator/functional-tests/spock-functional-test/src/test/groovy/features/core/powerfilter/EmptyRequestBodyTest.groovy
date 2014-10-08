package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Bug
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class EmptyRequestBodyTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.start()
    }

    def "when GET request with body, Deproxy should forward body"() {
        given:
        String requestBody = "request body"

        when:
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.targetPort}", requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody
    }

    @Unroll("#method should not have its body removed")
    def "Repose should not remove request bodies unless filters do so explicitly"() {
        given:
        String requestBody = "request body"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody

        where:
        method << ["PUT", "POST", "PATCH", "DELETE"]
    }

    @Category(Bug)
    @Unroll("bug - #method should not have its body removed")
    def "bug - Repose should not remove request bodies unless filters do so explicitly"() {
        given:
        String requestBody = "request body"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody

        where:
        method << ["GET"]
    }
}
