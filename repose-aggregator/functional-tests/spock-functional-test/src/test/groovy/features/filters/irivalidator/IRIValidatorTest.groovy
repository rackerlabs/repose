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

    def "When using iri-validator filter, Repose guards the request to the origin services"() {
        given:
        def path = "/" + requestpath + query

        when: "Request contains value(s) of the target header"
        def mc = deproxy.makeRequest(reposeEndpoint + path)

        then: "The x-forwarded-proto header is additionally added to the request going to the origin service"
        mc.receivedResponse.code == expectedCode
        if (reachedOrigin) mc.handlings[0] else !mc.handlings[0]

        where:
        method | requestpath | query    | expectedCode | reachedOrigin
        "GET"  | "test"      | "?a=b"   | "200"        | true
        "GET"  | "test"      | "?%aa=b" | "400"        | false
        "GET"  | "%aa"       | ""       | "400"        | false
    }
}
