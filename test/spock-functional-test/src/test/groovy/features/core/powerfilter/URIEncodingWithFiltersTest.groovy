package features.core.powerfilter
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingWithFiltersTest extends ReposeValveTest {


    def setupSpec() {

        repose.applyConfigs( "features/core/powerfilter/URIEncode/withIpIdentity" )
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }


    @Unroll("URI's with special character through Identity filters except API Validator sent = #URISent")
    def "URI's with special character through Identity filter"() {


        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent)

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size()>0
        messageChain.handlings.get(0).request.path.equals(URItoOriginService)


        where:
        URISent | URItoOriginService

        // space in the URI is not valid so returning 400's "/ messages?ids=+locations"  | "/messages?ids=+locations"
        "/messages?ids=+locations"  | "/messages?ids=+locations"
        "/+messages?ids=locations"  | "/+messages?ids=locations"
        "/messages?ids=locations"   | "/messages?ids=locations"

        "/messages?ids=;locations"  | "/messages?ids=%3Blocations"
        "/messages?ids=/locations"  | "/messages?ids=%2Flocations"
        "/messages?ids=?locations"  | "/messages?ids=%3Flocations"
        "/messages?ids=:locations"  | "/messages?ids=%3Alocations"
        "/messages?ids=@locations"  | "/messages?ids=%40locations"
        "/messages?ids==locations"  | "/messages?ids=%3Dlocations"
        "/messages?ids=,locations"  | "/messages?ids=%2Clocations"

        "/?messages?ids=locations"  | "/?messages%3Fids=locations"
        "//messages?ids=locations"  | "/messages?ids=locations"

        "/;messages?ids=locations"  | "/;messages?ids=locations"
        "/:messages?ids=locations"  | "/:messages?ids=locations"
        "/@messages?ids=locations"  | "/@messages?ids=locations"
        "/=messages?ids=locations"  | "/=messages?ids=locations"
        "/,messages?ids=locations"  | "/,messages?ids=locations"
        "/messages/add-nodes"       | "/messages/add-nodes"
        "/messages/add.nodes"       | "/messages/add.nodes"

    }


    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "URI's with special character through API Validator filter that gives 404"() {

        given:
        repose.updateConfigs( "features/core/powerfilter/URIEncode/withAPIValidator" )
        repose.waitForNon500FromUrl(reposeEndpoint)


        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent ,method: "GET", headers: ["X-Roles" : "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("404")

        where:
        URISent | URItoriginService
        "/messages/+add-nodes"     | "/messages/+add-nodes"
        "/+messages?ids=locations" | "/+messages?ids=locations"
        "/:messages?ids=locations" | "/:messages?ids=locations"



    }

    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "URI's with special character through API Validator filter that gives 200"() {



        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent ,method: "GET", headers: ["X-Roles" : "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size()>0
        messageChain.handlings.get(0).request.path.equals(URItoriginService)

        where:
        URISent | URItoriginService

        "/messages?ids=+locations"  | "/messages?ids=+locations"
        "/messages?ids=locations"   | "/messages?ids=locations"

        "/messages?ids=;locations"  | "/messages?ids=%3Blocations"
        "/messages?ids=/locations"  | "/messages?ids=%2Flocations"
        "/messages?ids=?locations"  | "/messages?ids=%3Flocations"
        "/messages?ids=:locations"  | "/messages?ids=%3Alocations"
        "/messages?ids=@locations"  | "/messages?ids=%40locations"
        "/messages?ids==locations"  | "/messages?ids=%3Dlocations"
        "/messages?ids=,locations"  | "/messages?ids=%2Clocations"

        "//messages?ids=locations"  | "/messages?ids=locations"
        "/;messages?ids=locations"  | "/;messages?ids=locations"
       "/@messages?ids=locations"   | "/@messages?ids=locations"
        "/=messages?ids=locations"  | "/=messages?ids=locations"
        "/,messages?ids=locations"  | "/,messages?ids=locations"
        "/messages/add-nodes"       | "/messages/add-nodes"
        "/messages/add.nodes"       | "/messages/add.nodes"


    }

    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "URI's with special character through API Validator filter that gives 405"() {

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent ,method: "GET", headers: ["X-Roles" : "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("405")

        where:
        URISent  | URItoriginService
        "/?messages?ids=locations"   | "/?messages?ids=locations"



    }


}