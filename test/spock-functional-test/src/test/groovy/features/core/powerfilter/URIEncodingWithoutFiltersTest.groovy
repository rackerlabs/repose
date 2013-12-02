package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingWithoutFiltersTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/powerfilter/URIEncode/noFilters")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Query components with allowed characters -> send to origin service -- #uri --> #expectedValue")
    def "Query components with allowed characters -> send to origin service"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue || messageChain.handlings[0].request.path == acceptableEncodedValue

        where:
        uri                         | expectedValue               | acceptableEncodedValue
        "/resource?name=value"      | "/resource?name=value"      | "/resource?name=value"
        "/resource?name%3Dvalue"    | "/resource?name=value"      | "/resource?name%3Dvalue"
        "/resource?name=abcdefghi"  | "/resource?name=abcdefghi"  | _
        "/resource?name=jklmnopqr"  | "/resource?name=jklmnopqr"  | _
        "/resource?name=stuvwxyz"   | "/resource?name=stuvwxyz"   | _
        "/resource?name=ABCDEFGHI"  | "/resource?name=ABCDEFGHI"  | _
        "/resource?name=JKLMNOPQR"  | "/resource?name=JKLMNOPQR"  | _
        "/resource?name=STUVWXYZ"   | "/resource?name=STUVWXYZ"   | _
        "/resource?name=0123456789" | "/resource?name=0123456789" | _
        "/resource?name=-"          | "/resource?name=-"          | "/resource?name=%2D"
        "/resource?name=."          | "/resource?name=."          | "/resource?name=%2E"
        "/resource?name=_"          | "/resource?name=_"          | "/resource?name=%5F"
        "/resource?name=~"          | "/resource?name=~"          | "/resource?name=%7E"


        "/resource?name=val!ue"     | "/resource?name=val!ue"     | "/resource?name=val%21ue"
        "/resource?name=val\$ue"    | "/resource?name=val\$ue"    | "/resource?name=val%24ue"
        "/resource?name=val&ue"     | "/resource?name=val&ue"     | "/resource?name=val%26ue"
        "/resource?name=val\'ue"    | "/resource?name=val\'ue"    | "/resource?name=val%27ue"
        "/resource?name=val(ue"     | "/resource?name=val(ue"     | "/resource?name=val%28ue"
        "/resource?name=val)ue"     | "/resource?name=val)ue"     | "/resource?name=val%29ue"
        "/resource?name=val*ue"     | "/resource?name=val*ue"     | "/resource?name=val%2Aue"
        "/resource?name=val+ue"     | "/resource?name=val+ue"     | "/resource?name=val%2Bue"
        "/resource?name=val,ue"     | "/resource?name=val,ue"     | "/resource?name=val%2Cue"
        "/resource?name=val;ue"     | "/resource?name=val;ue"     | "/resource?name=val%3Bue"
        "/resource?name=val=ue"     | "/resource?name=val=ue"     | "/resource?name=val%3Due"
        "/resource?name=val:ue"     | "/resource?name=val:ue"     | "/resource?name=val%3Aue"
        "/resource?name=val@ue"     | "/resource?name=val@ue"     | "/resource?name=val%40ue"
        "/resource?name=val/ue"     | "/resource?name=val/ue"     | "/resource?name=val%2Fue"
        "/resource?name=val?ue"     | "/resource?name=val?ue"     | "/resource?name=val%3Fue"
        "/resource?name=val%35ue"   | "/resource?name=val5ue"     | "/resource?name=val%35ue"

        /* percent-encoding the question mark means that it should not be
           interpreted as the beginning of the query component */
        "/resource%3Fname=value" | "/resource%3Fname=value" | _

        /* allowed characters that are percent-encoding are just as good. they
         SHOULD be decoded, but don't have to be */
        "/resource?name=val%2Due" | "/resource?name=val-ue" | "/resource?name=val%2Due"
        "/resource?name=val%2Eue" | "/resource?name=val.ue" | "/resource?name=val%2Eue"
        "/resource?name=val%5Fue" | "/resource?name=val_ue" | "/resource?name=val%5Fue"
        "/resource?name=val%7Eue" | "/resource?name=val~ue" | "/resource?name=val%7Eue"
        "/resource?name=val%21ue" | "/resource?name=val!ue" | "/resource?name=val%21ue"
        "/resource?name=val%24ue" | "/resource?name=val\$ue" | "/resource?name=val%24ue"
        "/resource?name=val%26ue" | "/resource?name=val&ue" | "/resource?name=val%26ue"
        "/resource?name=val%27ue" | "/resource?name=val\'ue" | "/resource?name=val%27ue"
        "/resource?name=val%28ue" | "/resource?name=val(ue" | "/resource?name=val%28ue"
        "/resource?name=val%29ue" | "/resource?name=val)ue" | "/resource?name=val%29ue"
        "/resource?name=val%2Aue" | "/resource?name=val*ue" | "/resource?name=val%2Aue"
        "/resource?name=val%2Bue" | "/resource?name=val+ue" | "/resource?name=val%2Bue"
        "/resource?name=val%2Cue" | "/resource?name=val,ue" | "/resource?name=val%2Cue"
        "/resource?name=val%3Bue" | "/resource?name=val;ue" | "/resource?name=val%3Bue"
        "/resource?name=val%3Due" | "/resource?name=val=ue" | "/resource?name=val%3Due"
        "/resource?name=val%3Aue" | "/resource?name=val:ue" | "/resource?name=val%3Aue"
        "/resource?name=val%40ue" | "/resource?name=val@ue" | "/resource?name=val%40ue"
        "/resource?name=val%2Fue" | "/resource?name=val/ue" | "/resource?name=val%2Fue"
        "/resource?name=val%3Fue" | "/resource?name=val?ue" | "/resource?name=val%3Fue"
        "/resource?name=val%35ue" | "/resource?name=val5ue" | "/resource?name=val%35ue"
    }

    def "When there are two question marks in the URI, the first should indicate the beginning of the query component"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue

        where:
        uri                         | expectedValue
        "/resource?name=some?value" | "/resource?name=some%3Fvalue"
    }

    @Unroll("Query components with disallowed characters that are percent-encoded -> send to origin service -- #uri ")
    def "Query components with disallowed characters that are percent-encoded -> send to origin service"() {

        // disallowed characters that are percent-encoded are acceptable, as long as they stay percent-encoded

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == uri

        where:
        uri                       | _
        "/resource?name=val%23ue" | _
        "/resource?name=val%5Bue" | _
        "/resource?name=val%5Due" | _
        "/resource?name=val%25ue" | _
        "/resource?name=val%60ue" | _
        "/resource?name=val%5Eue" | _
        "/resource?name=val%7Bue" | _
        "/resource?name=val%7Due" | _
        "/resource?name=val%5Cue" | _
        "/resource?name=val%7Cue" | _
        "/resource?name=val%22ue" | _
        "/resource?name=val%3Cue" | _
        "/resource?name=val%3Eue" | _
    }

    @Unroll("Query components with disallowed characters -> 400 response -- #uri")
    def "Query components with disallowed characters -> 400 response"() {

        when: "User sends request with a bad query component"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "400"
        messageChain.handlings.size() == 0

        where:
        uri                     | _
        "/resource?name=val#ue" | _
        "/resource?name=val[ue" | _
        "/resource?name=val]ue" | _
        "/resource?name=val%ue" | _
    }

    @Unroll("Query components with characters not mentioned in RFC 3986-> 400 response -- #uri")
    def "Query components with characters not mentioned in RFC 3986 -> 400 response"() {

        when: "User sends request with a bad query component"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "400"
        messageChain.handlings.size() == 0

        where:
        uri                      | _
        "/resource?name=val`ue"  | _
        "/resource?name=val^ue"  | _
        "/resource?name=val{ue"  | _
        "/resource?name=val}ue"  | _
        "/resource?name=val\\ue" | _
        "/resource?name=val|ue"  | _
        "/resource?name=val\"ue" | _
        "/resource?name=val<ue"  | _
        "/resource?name=val>ue"  | _
    }

    @Unroll("Query components with encoded forms of characters not mentioned in RFC 3986-> 200 response -- #uri")
    def "Query components with encoded forms of characters not mentioned in RFC 3986 -> 200 response"() {

        when: "User sends request with a bad query component"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == uri

        where:
        uri                       | _
        "/resource?name=val%60ue" | _
        "/resource?name=val%5Eue" | _
        "/resource?name=val%7Bue" | _
        "/resource?name=val%7Due" | _
        "/resource?name=val%5Cue" | _
        "/resource?name=val%7Cue" | _
        "/resource?name=val%22ue" | _
        "/resource?name=val%3Cue" | _
        "/resource?name=val%3Eue" | _
    }

    @Unroll("Query components with spaces disrupt the request line -> 400 response -- #uri")
    def "Query components with spaces disrupt the request line -> 400 response"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "400"
        messageChain.handlings.size() == 0

        where:
        uri                     | _
        "/resource?name=val ue" | _
        "/resource?na me=value" | _
    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}