package features.core.powerfilter
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingTest extends ReposeValveTest {


    def setupSpec() {


        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }



   @Unroll("URI's with special character through no filter sent = #URISent")
   def "URI's with special character through no filter(only destination filter)"() {
       setup:
       repose.applyConfigs( "features/core/powerfilter/URIEncode/noFilters" )
       repose.start()
       repose.waitForNon500FromUrl(reposeEndpoint)

       when: "User sends a request through repose"
       def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent)

       then: "Repose send the URI parameters without manipulation"
       messageChain.handlings.size()>0
       messageChain.handlings.get(0).request.path.equals(URISent)
       messageChain.receivedResponse.code.equals("200")

       cleanup:
       repose.stop()

       where:
       URISent | URItoOriginService
       "/messages?ids= locations"  | "/messages?ids= locations"
       "/messages?ids=+locations"  | "/messages?ids=+locations"
       "/ messages?ids=locations"  | "/ messages?ids=locations"
       "/+messages?ids=locations"  | "/+messages?ids=locations"
       "/messages?ids=locations"   | "/messages?ids=locations"


    }

    @Unroll("URI's with special character through API Validator filter sent = #URISent")
    def "URI's with special character through API Validator filter"() {

        setup:
        repose.applyConfigs( "features/core/powerfilter/URIEncode/withAPIValidator" )
        repose.start()


        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent ,method: "GET", headers: ["X-Roles" : "role-1"])

        then: "Repose send the URI parameters without manipulation"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size()>0
        messageChain.handlings.get(0).request.path.equals(URISent)

        cleanup:
        repose.stop()

        where:
        URISent | URItoriginService
        "/+messages?ids=locations"  | "/+messages?ids=locations"



    }


    @Unroll("URI's with special character through All filters except API Validator sent = #URISent")
    def "URI's with special character through All filters except API Validator"() {

        setup:
        repose.applyConfigs( "features/core/powerfilter/multifilters" )
        repose.start()

        when: "User sends a request through repose"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, path: URISent)

        then: "Repose send the URI parameters without manipulation"
        messageChain.handlings.size()>0
        messageChain.handlings.get(0).request.path.equals(URISent)
        messageChain.receivedResponse.code.equals("200")

        cleanup:
        repose.stop()

        where:
        URISent | URItoOriginService
        "/messages?ids= locations"  | "/messages?ids= locations"
        "/messages?ids=+locations"  | "/messages?ids=+locations"
        "/ messages?ids=locations"  | "/ messages?ids=locations"
        "/+messages?ids=locations"  | "/+messages?ids=locations"
        "/messages?ids=locations"   | "/messages?ids=locations"


    }

}