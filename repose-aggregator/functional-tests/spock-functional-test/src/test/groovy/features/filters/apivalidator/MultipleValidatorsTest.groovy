package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll


class MultipleValidatorsTest extends ReposeValveTest {

    def static badElementBody = "<a><testing>test</testing>Stuff</a>"
    def static badParamBody = "<element blah=\"something\"><testing>tests</testing></element>"
    def static badParamBadBody =  "<a blah=\"something\"><testing>test</testing>Stuff</a>"
    def static contentTypeHeader = ["content-type": "application/xml"]
    def static goodFirstBadSecondElementBody = "<test:element " +
            "xmlns:test=\"http://docs.openrepose.org/repose/common/api/v1.0\" blah=\"boblaw\">" +
            "</test:element>"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()

        repose.configurationProvider.applyConfigs("common",params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common",params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multiValidatorsPreProcess/", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            // TODO: Figure out a more elegant way for this test to shutdown.
            repose.stop(throwExceptionOnKill: false)
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

    def whenRequestPassesOnFirstValidatorButFailsSecond() {
        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST",
                requestBody: goodFirstBadSecondElementBody,
                headers: ["x-roles": "check-all, check-param"] + contentTypeHeader)

        then: "Request should be approved (will never reach 2nd validator)"
        messageChain.getReceivedResponse().code == "200"
        def sentRequest = messageChain.getHandlings()[0]

        and: "Origin service should receive request"
        sentRequest.getRequest().body.toString().
                contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<test:element xmlns:test=\"http://docs.openrepose.org/repose/common/api/v1.0\" blah=\"boblaw\"/>")
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
        badParamBadBody | ["x-roles": "check-param, check-all"] | "<message>Bad Content: Expecting the root element to be: test:element</message>"
    }
}
