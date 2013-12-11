package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy


class MultipleValidatorsTest extends ReposeValveTest{

    def body = """"<a blah=\"string\"><remove-me>test</remove-me>Stuff</a>"""

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common","features/filters/apivalidator/multiValidatorsPreProcess/")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }


    def whenRequestGoesThroughMultipleValidatorsWithPreprocessing(){

        when:
        def messageChain = deproxy.makeRequest(url: reposeEndpoint+"/resource", method: "POST",
                body: body, headers:["x-roles": "admin", "content-type": "application/xml"] )
        def sentRequest = messageChain.getHandlings()[0]

        then:
        messageChain.getReceivedResponse().code == "200"

        and:
        !sentRequest.getRequest().body.toString().contains("remove-me")

    }
}
