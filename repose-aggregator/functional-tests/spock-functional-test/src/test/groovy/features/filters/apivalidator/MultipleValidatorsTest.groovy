package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll


class MultipleValidatorsTest extends ReposeValveTest {

    def static badElementBody = "<a><testing>test</testing>Stuff</a>"
    def static badParamBody = "<element blah=\"something\"><testing>tests</testing></element>"
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
                requestBody: requestBody, headers: headers + contentTypeHeader)
        def sentRequest = messageChain.getHandlings()[0]

        then: "Request should return a 200"
        messageChain.getReceivedResponse().code == "200"

        and: "Origin service should receive request"
        sentRequest.getRequest().body.toString().contains(sentBody)
        where:
        requestBody    | headers                          | sentBody
        badParamBody   | ["x-roles": "check-param, pass"] | "blah=\"something\">"
        badElementBody | ["x-roles": "check-xsd, pass"]   | """<a>"""
        badElementBody | ["x-roles": "check-all,pass"]    | """<a>"""

    }

    @Unroll("Roles of #headers with request body of #requestBody should cause error #errorMessage")
    def whenRequestFailsBothValidators() {

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST",
                requestBody: requestBody, headers: headers + contentTypeHeader)

        then: "Request should be rejected"
        messageChain.getReceivedResponse().code == "400"
        messageChain.getHandlings().size() == 0

        and: "Message should return with reason"
        messageChain.getReceivedResponse().body.toString().contains(errorMessage)

        where:
        requestBody  | headers                               | errorMessage
        badParamBody | ["x-roles": "check-xsd,check-param"]  | "<message>Bad Content: blah should not be here</message>"
        badParamBody | ["x-roles": "check-param, check-all"] | "<message>Bad Content: blah should not be here</message>"
    }
}
