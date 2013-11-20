package features.core.powerfilter
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class URIEncodingWithoutFiltersTest extends ReposeValveTest {


    def setupSpec() {

        repose.applyConfigs( "features/core/powerfilter/URIEncode/noFilters" )
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
       messageChain.handlings.size()>0
       messageChain.handlings.get(0).request.path.equals(URItoOriginService)



       where:
       URISent | URItoOriginService
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

       "/:messages?ids=locations"  | "/:messages?ids=locations"
       "/@messages?ids=locations"  | "/@messages?ids=locations"
       "/=messages?ids=locations"  | "/=messages?ids=locations"
       "/,messages?ids=locations"  | "/,messages?ids=locations"
       "//messages?ids=locations"  | "/messages?ids=locations"
       "/;messages?ids=locations"  | "/;messages?ids=locations"

       "/?messages?ids=locations"  | "/?messages%3Fids=locations"


    }






}