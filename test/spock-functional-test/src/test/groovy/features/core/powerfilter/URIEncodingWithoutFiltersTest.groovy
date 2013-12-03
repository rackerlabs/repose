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
        uri                        | expectedValue              | acceptableEncodedValue
        "/resource?name=value"     | "/resource?name=value"     | "/resource?name=value"
        "/resource?name%3Dvalue"   | "/resource?name=value"     | "/resource?name%3Dvalue"


        "/resource?na0123me=value" | "/resource?na0123me=value" | "/resource?na0123me=value"
        "/resource?na4567me=value" | "/resource?na4567me=value" | "/resource?na4567me=value"
        "/resource?na89me=value"   | "/resource?na89me=value"   | "/resource?na89me=value"
        "/resource?naabcdme=value" | "/resource?naabcdme=value" | "/resource?naabcdme=value"
        "/resource?naefghme=value" | "/resource?naefghme=value" | "/resource?naefghme=value"
        "/resource?naijklme=value" | "/resource?naijklme=value" | "/resource?naijklme=value"
        "/resource?namnopme=value" | "/resource?namnopme=value" | "/resource?namnopme=value"
        "/resource?naqrstme=value" | "/resource?naqrstme=value" | "/resource?naqrstme=value"
        "/resource?nauvwxme=value" | "/resource?nauvwxme=value" | "/resource?nauvwxme=value"
        "/resource?nayzABme=value" | "/resource?nayzABme=value" | "/resource?nayzABme=value"
        "/resource?naCDEFme=value" | "/resource?naCDEFme=value" | "/resource?naCDEFme=value"
        "/resource?naGHIJme=value" | "/resource?naGHIJme=value" | "/resource?naGHIJme=value"
        "/resource?naKLMNme=value" | "/resource?naKLMNme=value" | "/resource?naKLMNme=value"
        "/resource?naOPQRme=value" | "/resource?naOPQRme=value" | "/resource?naOPQRme=value"
        "/resource?naSTUVme=value" | "/resource?naSTUVme=value" | "/resource?naSTUVme=value"
        "/resource?naWXYZme=value" | "/resource?naWXYZme=value" | "/resource?naWXYZme=value"

        "/resource?na-me=value"    | "/resource?na-me=value"    | "/resource?na%2Dme=value"
        "/resource?na.me=value"    | "/resource?na.me=value"    | "/resource?na%2Eme=value"
        "/resource?na_me=value"    | "/resource?na_me=value"    | "/resource?na%5Fme=value"
        "/resource?na~me=value"    | "/resource?na~me=value"    | "/resource?na%7Eme=value"

        "/resource?na!me=value"    | "/resource?na!me=value"    | "/resource?na%21me=value"
        "/resource?na\$me=value"   | "/resource?na\$me=value"   | "/resource?na%24me=value"
        "/resource?na&me=value"    | "/resource?na&me=value"    | "/resource?na%26me=value"
        "/resource?na\'me=value"   | "/resource?na\'me=value"   | "/resource?na%27me=value"
        "/resource?na(me=value"    | "/resource?na(me=value"    | "/resource?na%28me=value"
        "/resource?na)me=value"    | "/resource?na)me=value"    | "/resource?na%29me=value"
        "/resource?na*me=value"    | "/resource?na*me=value"    | "/resource?na%2Ame=value"
        "/resource?na+me=value"    | "/resource?na+me=value"    | "/resource?na%2Bme=value"
        "/resource?na,me=value"    | "/resource?na,me=value"    | "/resource?na%2Cme=value"
        "/resource?na;me=value"    | "/resource?na;me=value"    | "/resource?na%3Bme=value"
        "/resource?na=me=value"    | "/resource?na=me=value"    | "/resource?na%3Dme=value"
        "/resource?na:me=value"    | "/resource?na:me=value"    | "/resource?na%3Ame=value"
        "/resource?na@me=value"    | "/resource?na@me=value"    | "/resource?na%40me=value"
        "/resource?na/me=value"    | "/resource?na/me=value"    | "/resource?na%2Fme=value"
        "/resource?na?me=value"    | "/resource?na?me=value"    | "/resource?na%3Fme=value"

        "/resource?name=val0123ue" | "/resource?name=val0123ue" | "/resource?name=val0123ue"
        "/resource?name=val4567ue" | "/resource?name=val4567ue" | "/resource?name=val4567ue"
        "/resource?name=val89ue"   | "/resource?name=val89ue"   | "/resource?name=val89ue"
        "/resource?name=valabcdue" | "/resource?name=valabcdue" | "/resource?name=valabcdue"
        "/resource?name=valefghue" | "/resource?name=valefghue" | "/resource?name=valefghue"
        "/resource?name=valijklue" | "/resource?name=valijklue" | "/resource?name=valijklue"
        "/resource?name=valmnopue" | "/resource?name=valmnopue" | "/resource?name=valmnopue"
        "/resource?name=valqrstue" | "/resource?name=valqrstue" | "/resource?name=valqrstue"
        "/resource?name=valuvwxue" | "/resource?name=valuvwxue" | "/resource?name=valuvwxue"
        "/resource?name=valyzABue" | "/resource?name=valyzABue" | "/resource?name=valyzABue"
        "/resource?name=valCDEFue" | "/resource?name=valCDEFue" | "/resource?name=valCDEFue"
        "/resource?name=valGHIJue" | "/resource?name=valGHIJue" | "/resource?name=valGHIJue"
        "/resource?name=valKLMNue" | "/resource?name=valKLMNue" | "/resource?name=valKLMNue"
        "/resource?name=valOPQRue" | "/resource?name=valOPQRue" | "/resource?name=valOPQRue"
        "/resource?name=valSTUVue" | "/resource?name=valSTUVue" | "/resource?name=valSTUVue"
        "/resource?name=valWXYZue" | "/resource?name=valWXYZue" | "/resource?name=valWXYZue"

        "/resource?name=val-ue"    | "/resource?name=val-ue"    | "/resource?name=val%2Due"
        "/resource?name=val.ue"    | "/resource?name=val.ue"    | "/resource?name=val%2Eue"
        "/resource?name=val_ue"    | "/resource?name=val_ue"    | "/resource?name=val%5Fue"
        "/resource?name=val~ue"    | "/resource?name=val~ue"    | "/resource?name=val%7Eue"

        "/resource?name=val!ue"    | "/resource?name=val!ue"    | "/resource?name=val%21ue"
        "/resource?name=val\$ue"   | "/resource?name=val\$ue"   | "/resource?name=val%24ue"
        "/resource?name=val&ue"    | "/resource?name=val&ue"    | "/resource?name=val%26ue"
        "/resource?name=val\'ue"   | "/resource?name=val\'ue"   | "/resource?name=val%27ue"
        "/resource?name=val(ue"    | "/resource?name=val(ue"    | "/resource?name=val%28ue"
        "/resource?name=val)ue"    | "/resource?name=val)ue"    | "/resource?name=val%29ue"
        "/resource?name=val*ue"    | "/resource?name=val*ue"    | "/resource?name=val%2Aue"
        "/resource?name=val+ue"    | "/resource?name=val+ue"    | "/resource?name=val%2Bue"
        "/resource?name=val,ue"    | "/resource?name=val,ue"    | "/resource?name=val%2Cue"
        "/resource?name=val;ue"    | "/resource?name=val;ue"    | "/resource?name=val%3Bue"
        "/resource?name=val=ue"    | "/resource?name=val=ue"    | "/resource?name=val%3Due"
        "/resource?name=val:ue"    | "/resource?name=val:ue"    | "/resource?name=val%3Aue"
        "/resource?name=val@ue"    | "/resource?name=val@ue"    | "/resource?name=val%40ue"
        "/resource?name=val/ue"    | "/resource?name=val/ue"    | "/resource?name=val%2Fue"
        "/resource?name=val?ue"    | "/resource?name=val?ue"    | "/resource?name=val%3Fue"
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
        "/resource?na%30%31%32%33me=value" | "/resource?na0123me=value" | "/resource?na0123me=value"
        "/resource?na%34%35%36%37me=value" | "/resource?na4567me=value" | "/resource?na4567me=value"
        "/resource?na%38%39me=value"       | "/resource?na89me=value"   | "/resource?na89me=value"
        "/resource?na%61%62%63%64me=value" | "/resource?naabcdme=value" | "/resource?naabcdme=value"
        "/resource?na%65%66%67%68me=value" | "/resource?naefghme=value" | "/resource?naefghme=value"
        "/resource?na%69%6A%6B%6Cme=value" | "/resource?naijklme=value" | "/resource?naijklme=value"
        "/resource?na%6D%6E%6F%70me=value" | "/resource?namnopme=value" | "/resource?namnopme=value"
        "/resource?na%71%72%73%74me=value" | "/resource?naqrstme=value" | "/resource?naqrstme=value"
        "/resource?na%75%76%77%78me=value" | "/resource?nauvwxme=value" | "/resource?nauvwxme=value"
        "/resource?na%79%7A%41%42me=value" | "/resource?nayzABme=value" | "/resource?nayzABme=value"
        "/resource?na%43%44%45%46me=value" | "/resource?naCDEFme=value" | "/resource?naCDEFme=value"
        "/resource?na%47%48%49%4Ame=value" | "/resource?naGHIJme=value" | "/resource?naGHIJme=value"
        "/resource?na%4B%4C%4D%4Eme=value" | "/resource?naKLMNme=value" | "/resource?naKLMNme=value"
        "/resource?na%4F%50%51%52me=value" | "/resource?naOPQRme=value" | "/resource?naOPQRme=value"
        "/resource?na%53%54%55%56me=value" | "/resource?naSTUVme=value" | "/resource?naSTUVme=value"
        "/resource?na%57%58%59%5Ame=value" | "/resource?naWXYZme=value" | "/resource?naWXYZme=value"
        "/resource?na%2Dme=value"          | "/resource?na-me=value"    | "/resource?na%2Dme=value"
        "/resource?na%2Eme=value"          | "/resource?na.me=value"    | "/resource?na%2Eme=value"
        "/resource?na%5Fme=value"          | "/resource?na_me=value"    | "/resource?na%5Fme=value"
        "/resource?na%7Eme=value"          | "/resource?na~me=value"    | "/resource?na%7Eme=value"
        "/resource?na%21me=value"          | "/resource?na!me=value"    | "/resource?na%21me=value"
        "/resource?na%24me=value"          | "/resource?na\$me=value"   | "/resource?na%24me=value"
        "/resource?na%26me=value"          | "/resource?na&me=value"    | "/resource?na%26me=value"
        "/resource?na%27me=value"          | "/resource?na\'me=value"   | "/resource?na%27me=value"
        "/resource?na%28me=value"          | "/resource?na(me=value"    | "/resource?na%28me=value"
        "/resource?na%29me=value"          | "/resource?na)me=value"    | "/resource?na%29me=value"
        "/resource?na%2Ame=value"          | "/resource?na*me=value"    | "/resource?na%2Ame=value"
        "/resource?na%2Bme=value"          | "/resource?na+me=value"    | "/resource?na%2Bme=value"
        "/resource?na%2Cme=value"          | "/resource?na,me=value"    | "/resource?na%2Cme=value"
        "/resource?na%3Bme=value"          | "/resource?na;me=value"    | "/resource?na%3Bme=value"
        "/resource?na%3Dme=value"          | "/resource?na=me=value"    | "/resource?na%3Dme=value"
        "/resource?na%3Ame=value"          | "/resource?na:me=value"    | "/resource?na%3Ame=value"
        "/resource?na%40me=value"          | "/resource?na@me=value"    | "/resource?na%40me=value"
        "/resource?na%2Fme=value"          | "/resource?na/me=value"    | "/resource?na%2Fme=value"
        "/resource?na%3Fme=value"          | "/resource?na?me=value"    | "/resource?na%3Fme=value"
        "/resource?na%35me=value"          | "/resource?na5me=value"    | "/resource?na%35me=value"


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

        "/resource?na%4b%4c%4d%4eme=value" | "/resource?naKLMNme=value" | "/resource?naKLMNme=value"
        "/resource?name=val%4b%4c%4d%4eue" | "/resource?name=valKLMNue" | "/resource?name=valKLMNue"

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
        "/resource?na%23me=value" | _
        "/resource?na%5Bme=value" | _
        "/resource?na%5Dme=value" | _
        "/resource?na%25me=value" | _
        "/resource?na%60me=value" | _
        "/resource?na%5Eme=value" | _
        "/resource?na%7Bme=value" | _
        "/resource?na%7Dme=value" | _
        "/resource?na%5Cme=value" | _
        "/resource?na%7Cme=value" | _
        "/resource?na%22me=value" | _
        "/resource?na%3Cme=value" | _
        "/resource?na%3Eme=value" | _
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
        "/resource?na#me=value" | _
        "/resource?na[me=value" | _
        "/resource?na]me=value" | _
        "/resource?na%me=value" | _
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
        "/resource?na`me=value"  | _
        "/resource?na^me=value"  | _
        "/resource?na{me=value"  | _
        "/resource?na}me=value"  | _
        "/resource?na\\me=value" | _
        "/resource?na|me=value"  | _
        "/resource?na\"me=value" | _
        "/resource?na<me=value"  | _
        "/resource?na>me=value"  | _
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
        "/resource?na%60me=value" | _
        "/resource?na%5Eme=value" | _
        "/resource?na%7Bme=value" | _
        "/resource?na%7Dme=value" | _
        "/resource?na%5Cme=value" | _
        "/resource?na%7Cme=value" | _
        "/resource?na%22me=value" | _
        "/resource?na%3Cme=value" | _
        "/resource?na%3Eme=value" | _
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