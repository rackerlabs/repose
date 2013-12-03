package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingWithIpIdentityTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/core/powerfilter/URIEncode/withIpIdentity")
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
        "/resource?name=abcdefghi"  | "/resource?name=abcdefghi"  | "/resource?name=abcdefghi"
        "/resource?name=jklmnopqr"  | "/resource?name=jklmnopqr"  | "/resource?name=jklmnopqr"
        "/resource?name=stuvwxyz"   | "/resource?name=stuvwxyz"   | "/resource?name=stuvwxyz"
        "/resource?name=ABCDEFGHI"  | "/resource?name=ABCDEFGHI"  | "/resource?name=ABCDEFGHI"
        "/resource?name=JKLMNOPQR"  | "/resource?name=JKLMNOPQR"  | "/resource?name=JKLMNOPQR"
        "/resource?name=STUVWXYZ"   | "/resource?name=STUVWXYZ"   | "/resource?name=STUVWXYZ"
        "/resource?name=0123456789" | "/resource?name=0123456789" | "/resource?name=0123456789"
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
    }

    @Unroll("Query components with percent-encoded allowed characters -> send to origin service -- #uri --> #expectedValue")
    def "Query components with percent-encoded allowed characters -> send to origin service"() {

        // allowed characters that are percent-encoding are just as good. they
        // SHOULD be decoded, but don't have to be.

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue || messageChain.handlings[0].request.path == acceptableEncodedValue

        where:
        uri                                | expectedValue              | acceptableEncodedValue
        "/resource?name=val%30%31%32%33ue" | "/resource?name=val0123ue" | "/resource?name=val0123ue"
        "/resource?name=val%34%35%36%37ue" | "/resource?name=val4567ue" | "/resource?name=val4567ue"
        "/resource?name=val%38%39ue"       | "/resource?name=val89ue"   | "/resource?name=val89ue"
        "/resource?name=val%61%62%63%64ue" | "/resource?name=valabcdue" | "/resource?name=valabcdue"
        "/resource?name=val%65%66%67%68ue" | "/resource?name=valefghue" | "/resource?name=valefghue"
        "/resource?name=val%69%6A%6B%6Cue" | "/resource?name=valijklue" | "/resource?name=valijklue"
        "/resource?name=val%6D%6E%6F%70ue" | "/resource?name=valmnopue" | "/resource?name=valmnopue"
        "/resource?name=val%71%72%73%74ue" | "/resource?name=valqrstue" | "/resource?name=valqrstue"
        "/resource?name=val%75%76%77%78ue" | "/resource?name=valuvwxue" | "/resource?name=valuvwxue"
        "/resource?name=val%79%7A%41%42ue" | "/resource?name=valyzABue" | "/resource?name=valyzABue"
        "/resource?name=val%43%44%45%46ue" | "/resource?name=valCDEFue" | "/resource?name=valCDEFue"
        "/resource?name=val%47%48%49%4Aue" | "/resource?name=valGHIJue" | "/resource?name=valGHIJue"
        "/resource?name=val%4B%4C%4D%4Eue" | "/resource?name=valKLMNue" | "/resource?name=valKLMNue"
        "/resource?name=val%4F%50%51%52ue" | "/resource?name=valOPQRue" | "/resource?name=valOPQRue"
        "/resource?name=val%53%54%55%56ue" | "/resource?name=valSTUVue" | "/resource?name=valSTUVue"
        "/resource?name=val%57%58%59%5Aue" | "/resource?name=valWXYZue" | "/resource?name=valWXYZue"
        "/resource?name=val%2Due"          | "/resource?name=val-ue"    | "/resource?name=val%2Due"
        "/resource?name=val%2Eue"          | "/resource?name=val.ue"    | "/resource?name=val%2Eue"
        "/resource?name=val%5Fue"          | "/resource?name=val_ue"    | "/resource?name=val%5Fue"
        "/resource?name=val%7Eue"          | "/resource?name=val~ue"    | "/resource?name=val%7Eue"
        "/resource?name=val%21ue"          | "/resource?name=val!ue"    | "/resource?name=val%21ue"
        "/resource?name=val%24ue"          | "/resource?name=val\$ue"   | "/resource?name=val%24ue"
        "/resource?name=val%26ue"          | "/resource?name=val&ue"    | "/resource?name=val%26ue"
        "/resource?name=val%27ue"          | "/resource?name=val\'ue"   | "/resource?name=val%27ue"
        "/resource?name=val%28ue"          | "/resource?name=val(ue"    | "/resource?name=val%28ue"
        "/resource?name=val%29ue"          | "/resource?name=val)ue"    | "/resource?name=val%29ue"
        "/resource?name=val%2Aue"          | "/resource?name=val*ue"    | "/resource?name=val%2Aue"
        "/resource?name=val%2Bue"          | "/resource?name=val+ue"    | "/resource?name=val%2Bue"
        "/resource?name=val%2Cue"          | "/resource?name=val,ue"    | "/resource?name=val%2Cue"
        "/resource?name=val%3Bue"          | "/resource?name=val;ue"    | "/resource?name=val%3Bue"
        "/resource?name=val%3Due"          | "/resource?name=val=ue"    | "/resource?name=val%3Due"
        "/resource?name=val%3Aue"          | "/resource?name=val:ue"    | "/resource?name=val%3Aue"
        "/resource?name=val%40ue"          | "/resource?name=val@ue"    | "/resource?name=val%40ue"
        "/resource?name=val%2Fue"          | "/resource?name=val/ue"    | "/resource?name=val%2Fue"
        "/resource?name=val%3Fue"          | "/resource?name=val?ue"    | "/resource?name=val%3Fue"
        "/resource?name=val%35ue"          | "/resource?name=val5ue"    | "/resource?name=val%35ue"
    }

    def "When there are two question marks in the URI, the first should indicate the beginning of the query component"() {

        given:
        String uri = "/resource?name=some?value"
        String expectedValue = "/resource?name=some%3Fvalue"

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue

    }

    def "A percent-encoded question mark should not be interpreted as the beginning of the query component"() {

        given:
        String uri = "/resource%3Fname=value"
        String expectedValue = "/resource%3Fname=value"

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue
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