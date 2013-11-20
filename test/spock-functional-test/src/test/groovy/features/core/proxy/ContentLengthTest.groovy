package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ContentLengthTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/proxy", "features/services/httpconnectionpool/chunkedfalse")
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

    @Unroll("When set to #method chunked encoding to false and sending #reqBody.")
    def "When set to send chunked encoding to false. Repose should not send requests chunked"() {

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: method, requestBody: reqBody])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == transfer_encoding
        ((Handling) sentRequest).request.getHeaders().findAll("content-type").size() == content_type

        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == content_length

        if(content_length > 0)
            assert ((Handling) sentRequest).request.getHeaders().getFirstValue("content-length").equalsIgnoreCase((reqBody == null) ? "0" : reqBody.length().toString())


        where:
        method | reqBody | content_type | content_length | transfer_encoding
        "POST" | "blah"  | 1            | 1              | 0
        "POST" | null    | 0            | 1              | 0
        "PUT"  | "blah"  | 1            | 1              | 0
        "PUT"  | null    | 0            | 1              | 0
        "TRACE"| "blah"  | 1            | 0              | 0
        "TRACE"| null    | 0            | 0              | 0

    }

}
