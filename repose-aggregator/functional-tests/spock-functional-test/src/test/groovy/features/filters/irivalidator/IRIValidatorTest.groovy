package features.filters.irivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/**
 * Created by jcombs on 1/12/15.
 */
class IRIValidatorTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/irivalidator", params)
        repose.start(waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    def "When using iri-validator filter, Repose guards the request to the origin services" () {
        given:
        def Map headers = ["x-rax-user": "test-user-a", "x-rax-groups": "reposegroup11"]
        def path = "/" + requestpath + "/?" + query;

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest([url: reposeEndpoint + path, headers: headers])


        then: "The x-forwarded-proto header is additionally added to the request going to the origin service"
        mc.receivedResponse.code == expectedCode
        mc.getSentRequest().getHeaders().contains("x-rax-user")
        mc.getSentRequest().getHeaders().getFirstValue("x-rax-user") == "test-user-a"
        mc.getSentRequest().getHeaders().contains("x-forwarded-proto") == false
        mc.handlings[0].request.headers.contains("x-rax-user")
        mc.handlings[0].request.headers.getFirstValue("x-rax-user") == "test-user-a"
        mc.handlings[0].request.headers.contains("x-forwarded-proto")
        String forwardedProto = mc.handlings[0].request.headers.getFirstValue("x-forwarded-proto")
        forwardedProto.toLowerCase().contains("http")

        where:
        method   | requestpath     | query     | expectedCode
        "GET"    | "test"          |"a=b"      | "200"
        "GET"    | "test"          |"%%a=b"    | "500?"
    }
}
