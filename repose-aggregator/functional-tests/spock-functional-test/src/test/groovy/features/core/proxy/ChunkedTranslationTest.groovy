package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain

/**
 * User: dimi5963
 * Date: 9/24/13
 * Time: 3:36 PM
 */
class ChunkedTranslationTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/translation/common",
                "features/filters/translation/request"
                , "features/services/httpconnectionpool/chunkedfalse")
        repose.start()
    }

    def cleanupSpec(){
        if (deproxy) {
            deproxy.shutdown()
        }
        if (repose) {
            repose.stop()
        }

    }

    def "When set to send chunked encoding to false. Repose should send content length of translated request"() {
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

