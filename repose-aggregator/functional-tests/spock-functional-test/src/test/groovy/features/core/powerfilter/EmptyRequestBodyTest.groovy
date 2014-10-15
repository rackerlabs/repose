package features.core.powerfilter

import framework.ReposeValveTest
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

    def "Deproxy should not remove the body of a HTTP request"() {
        given:
        String requestBody = "request body"
        String deproxyEndpoint = "http://localhost:${properties.targetPort}"

        when:
        MessageChain mc = deproxy.makeRequest(url: deproxyEndpoint, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == requestBody

        where:
        method << ["GET", "HEAD", "PUT", "POST", "PATCH", "DELETE"]
    }

    // NOTE: The following test has proven contentious. In accordance with RFC 2616 Section 5, all HTTP requests may
    // contain a request body. However, in the words of Roy Fielding:
    // "Server semantics for GET, however, are restricted such that a body, if any, has no semantic meaning to the
    // request. The requirements on parsing are separate from the requirements on method semantics."
    // It has been decided that Repose will remove the request body for certain request types. No one should care.
    // Anyone who does care has either written an invalid HTTP service or is a disputatious individual.
    @Unroll("#method should have its body removed")
    def "Repose should remove request bodies"() {
        given:
        String requestBody = "request body"

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, requestBody: requestBody)

        then:
        mc.getSentRequest().getBody().toString() == requestBody
        mc.getHandlings().get(0).getRequest().getBody().toString() == ""

        where:
        method << ["GET", "HEAD"]
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
}
