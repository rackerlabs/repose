package features.core.powerfilter

import framework.ReposeValveTest
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll
import org.rackspace.gdeproxy.Header

/**
 * Setup: the configuration for this test has a container.cfg.xml with a content-body-read-limit="32000"
 */
class RequestSizeTest extends ReposeValveTest {

    String charset = (('A'..'Z') + ('0'..'9')).join()

    def setupSpec() {
        repose.applyConfigs("features/core/powerfilter/requestsize")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("request with header size of #headerSize should respond with 413")
    def "max header size allowed is not influenced by content-body-read-limit"() {

        given: "I have headers that exceed the header size limit"
        def header1 = RandomStringUtils.random(headerSize, charset)

        when: "I send a request to REPOSE with my headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [headerName: header1])

        then: "I get a response of 413"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0

        where:
        headerName | headerSize
        "Header1"  | 32000
        "Header1"  | 8000
        "Header1"  | 6000
    }

    @Unroll("request with header size of #headerSize should respond with 200")
    def "headers within jetty default size limit are allowed through"() {

        given: "I have headers that are within the header size limit"
        int defaultHeadersSize = 0
        MessageChain fmc = deproxy.makeRequest(url: reposeEndpoint)
        for(Header hdr : fmc.sentRequest.headers._headers){
            defaultHeadersSize += hdr.value.length()
        }
        int largeHeaderSize = headerSize - defaultHeadersSize
        def header1 = RandomStringUtils.random(largeHeaderSize, charset)

        when: "I send a request to REPOSE with my headers"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: [headerName: header1])

        then: "I get a response of 200"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        where:
        headerName | headerSize
        "Header1"  | 5980
        "Header1"  | 5000
        "Header1"  | 4500
        "Header1"  | 4000
    }


}
