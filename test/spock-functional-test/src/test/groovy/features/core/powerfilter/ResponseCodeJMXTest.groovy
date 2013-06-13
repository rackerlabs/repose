package features.filters.apivalidator
import framework.ReposeValveTest

class ResponseCodeJMXTest extends ReposeValveTest {

    String jmxStart = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""
    String jmxEnd = "\",name=\"5XX\""
    String allName = jmxStart + "All Endpoints" + jmxEnd
    String reposeName = jmxStart + "Repose" + jmxEnd

    def setup() {
        repose.applyConfigs(
                "features/core/powerfilter/common" )
        repose.enableJmx(true)
        repose.start()

    }

    def cleanup() {
        repose.stop()
    }

    def "when sending requests, Repose & endpoint response codes should be recorded"() {

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.doGet( "/endpoint", new HashMap() );
        deproxy.doGet( "/endpoint", new HashMap() );
        deproxy.doGet( "/cluster", new HashMap() );

        def reposeCount = repose.jmx.getMBeanAttribute( reposeName, "Count" )

        /**
         *  TODO:
         *
         *  1) Return counts for different types of return codes
         *  2) Need Deproxy is create endpoint service
         *  3) Need to verify counts for:
         *     - endpoint
         *     - cluster
         *     - All Endpoints
         */

        then:
        reposeCount == 4
    }
}
