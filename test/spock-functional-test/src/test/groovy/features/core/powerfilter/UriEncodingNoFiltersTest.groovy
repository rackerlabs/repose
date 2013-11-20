package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingNoFiltersTest extends ReposeValveTest {


    def setupSpec() {

        repose.applyConfigs("features/core/powerfilter/URIEncode/noFilters")
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }



    @Unroll("URI's with reserved character through no filter sent = #URISent")
    def "URI's with reserved character(+) through no filter(only destination filter)"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() > 0
        messageChain.handlings.get(0).request.path.equals(URItoOriginService)



        where:
        URISent                    | URItoOriginService
        "/messages?ids=+locations" | "/messages?ids=+locations"
        "/+messages?ids=locations" | "/+messages?ids=locations"
        "/messages?ids=locations"  | "/messages?ids=locations"

        "/messages?ids=;locations" | "/messages?ids=;locations"
        "/messages?ids=/locations" | "/messages?ids=/locations"
        "/messages?ids=?locations" | "/messages?ids=?locations"
        "/messages?ids=:locations" | "/messages?ids=:locations"
        "/messages?ids=@locations" | "/messages?ids=@locations"
        "/messages?ids==locations" | "/messages?ids==locations"
        "/messages?ids=,locations" | "/messages?ids=,locations"

        "/:messages?ids=locations" | "/:messages?ids=locations"
        "/@messages?ids=locations" | "/@messages?ids=locations"
        "/=messages?ids=locations" | "/=messages?ids=locations"
        "/,messages?ids=locations" | "/,messages?ids=locations"
        "//messages?ids=locations" | "//messages?ids=locations"
        "/;messages?ids=locations" | "/;messages?ids=locations"

        "/?messages?ids=locations" | "/?messages%3Fids=locations"


    }


    @Unroll("Query components with allowed characters -> send to origin service -- #uri")
    def "Query components with allowed characters -> send to origin service"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code == "200"
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.path == expectedValue || messageChain.handlings[0].request.path == acceptableEncodedValue



        where:
        uri                       | expectedValue             | acceptableEncodedValue
        "/resource?name=value"    | "/resource?name=value"    | "/resource?name=value"
        "/resource?name%3Dvalue"  | "/resource?name=value"    | "/resource?name%3Dvalue"
        "/resource%3Fname=value"  | "/resource%3Fname=value"  | _
        "/resource?name=val-ue"   | "/resource?name=val-ue"   | "/resource?name=val%2Due"
        "/resource?name=val.ue"   | "/resource?name=val.ue"   | "/resource?name=val%2Eue"
        "/resource?name=val_ue"   | "/resource?name=val_ue"   | "/resource?name=val%5Fue"
        "/resource?name=val~ue"   | "/resource?name=val~ue"   | "/resource?name=val%7Eue"
        "/resource?name=val!ue"   | "/resource?name=val!ue"   | "/resource?name=val%21ue"
        "/resource?name=val\$ue"  | "/resource?name=val\$ue"  | "/resource?name=val%24ue"
        "/resource?name=val&ue"   | "/resource?name=val&ue"   | "/resource?name=val%26ue"
        "/resource?name=val\'ue"  | "/resource?name=val\'ue"  | "/resource?name=val%27ue"
        "/resource?name=val(ue"   | "/resource?name=val(ue"   | "/resource?name=val%28ue"
        "/resource?name=val)ue"   | "/resource?name=val)ue"   | "/resource?name=val%29ue"
        "/resource?name=val*ue"   | "/resource?name=val*ue"   | "/resource?name=val%2Aue"
        "/resource?name=val+ue"   | "/resource?name=val+ue"   | "/resource?name=val%2Bue"
        "/resource?name=val,ue"   | "/resource?name=val,ue"   | "/resource?name=val%2Cue"
        "/resource?name=val;ue"   | "/resource?name=val;ue"   | "/resource?name=val%3Bue"
        "/resource?name=val=ue"   | "/resource?name=val=ue"   | "/resource?name=val%3Due"
        "/resource?name=val:ue"   | "/resource?name=val:ue"   | "/resource?name=val%3Aue"
        "/resource?name=val@ue"   | "/resource?name=val@ue"   | "/resource?name=val%40ue"
        "/resource?name=val/ue"   | "/resource?name=val/ue"   | "/resource?name=val%2Fue"
        "/resource?name=val?ue"   | "/resource?name=val?ue"   | "/resource?name=val%3Fue"
        "/resource?name=val%35ue" | "/resource?name=val5ue"   | "/resource?name=val%35ue"
        "/resource?name=val%23ue" | "/resource?name=val%23ue" | _
        "/resource?name=val%5Bue" | "/resource?name=val%5Bue" | _
        "/resource?name=val%5Due" | "/resource?name=val%5Due" | _
        "/resource?name=val%25ue" | "/resource?name=val%25ue" | _
        "/resource?name=val%60ue" | "/resource?name=val%60ue" | _
        "/resource?name=val%5Eue" | "/resource?name=val%5Eue" | _
        "/resource?name=val%7Bue" | "/resource?name=val%7Bue" | _
        "/resource?name=val%7Due" | "/resource?name=val%7Due" | _
        "/resource?name=val%5Cue" | "/resource?name=val%5Cue" | _
        "/resource?name=val%7Cue" | "/resource?name=val%7Cue" | _
        "/resource?name=val%22ue" | "/resource?name=val%22ue" | _
        "/resource?name=val%3Cue" | "/resource?name=val%3Cue" | _
        "/resource?name=val%3Eue" | "/resource?name=val%3Eue" | _
    }

    @Unroll("Query components with disallowed characters -> 400 response -- #uri")
    def "Query components with disallowed characters -> 400 response"() {

        when: "User sends request with a bad query component"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: uri)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "400"
        messageChain.handlings.size() == 0

        where:
        uri                      | _
        "/resource?name=val#ue"  | _
        "/resource?name=val[ue"  | _
        "/resource?name=val]ue"  | _
        "/resource?name=val%ue"  | _
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


    @Unroll("Query components with spaces disrupt the request line -> 400 response -- #URISent")
    def "Query components with spaces disrupt the request line -> 400 response"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent)

        then: "Repose returns an error"
        messageChain.receivedResponse.code == "400"
        messageChain.handlings.size() == 0



        where:
        URISent                 | _
        "/resource?name=val ue" | _
        "/resource?na me=value" | _
    }





}