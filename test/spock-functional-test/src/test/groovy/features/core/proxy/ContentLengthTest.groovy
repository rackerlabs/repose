package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain

class ContentLengthTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }


    def "When set to send chunked encoding to false. Repose should not send requests chunked"() {

        given:
        repose.applyConfigs("features/core/proxy", "features/services/httpconnectionpool/chunkedfalse")
        repose.start()

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "POST", requestBody: "blah"])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == 0

        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == 1

        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length").equalsIgnoreCase("4")


    }

    def "When set to send chunked encoding to false. Repose should send content length of translated request"() {

        given:
        repose.applyConfigs("features/filters/translation/common",
                "features/filters/translation/request"
                , "features/services/httpconnectionpool/chunkedfalse")

        repose.start()
        def reqHeaders = ["accept": "application/xml", "content-type": "application/xml"]

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: "POST", requestBody: xmlPayLoad,
        headers: reqHeaders])

        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then: "repose should not sending any transfer encoding header"
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == 0

        and: "repose should send a content-length with the request"
        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == 1

        and: "new content length should not match that of the original request"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length") == "15"
        ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length") != messageChain.sentRequest.headers.getFirstValue("content-length")

    }
}
