package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll


class MultipleValidatorsTest extends ReposeValveTest {

    def static badElementBody = """"<a><testing>test</testing>Stuff</a>"""
    def static badParamBody = """<element blah=\"string\" > <testing>test</testing></element>"""
    def static contentTypeHeader = ["content-type": "application/xml"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/multiValidatorsPreProcess/")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("Roles of #headers with request body of #requestBody should pass through second validator")
    def whenRequestFailsOnFirstValidatorButPassesSecond() {

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST",
                body: requestBody, headers: headers + contentTypeHeader)
        def sentRequest = messageChain.getHandlings()[0]

        then: "Request should return a 200"
        messageChain.getReceivedResponse().code == "200"

        and: "Origin service should receive request"
        sentRequest.getRequest().body.toString().equals(requestBody)

        where:
        requestBody    | headers
        badParamBody   | ["x-roles": "param-check,pass"]
        badElementBody | ["x-roles": "xsd-check, pass"]
        badElementBody | ["x-roles": "check_all,pass"]

    }

    @Unroll("Roles of #headers with request body of #requestBody should cause error #errorMessage")
    def whenRequestFailsBothValidators() {

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST",
                body: requestBody, headers: headers + contentTypeHeader)

        then: "Request should be rejected"
        messageChain.getReceivedResponse().code == "400"
        messageChain.getHandlings().size() == 0

        and: "Message should return with reason"
        messageChain.getReceivedResponse().body.toString().contains(errorMessage)

        where:
        requestBody    | headers                               | errorMessage
        badParamBody   | ["x-roles": "xsd-check,param-check"]  | "<message>Bad Content: blah should not be here</message>"
        badElementBody | ["x-roles": "param-check, check-all"] | "<message>Bad Content: blah should not be here</message>"
    }
}
