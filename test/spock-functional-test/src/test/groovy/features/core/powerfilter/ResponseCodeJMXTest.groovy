package features.filters.apivalidator
import framework.ReposeValveTest

class ResponseCodeJMXTest extends ReposeValveTest {

    String jmxStart = "\"repose-node1-com.rackspace.papi\":type=\"ResponseCode\",scope=\""
    String jmxEnd = "\",name=\"5XX\""
    String allName = jmxStart + "All Endpoints" + jmxEnd
    String reposeName = jmxStart + "Repose" + jmxEnd

    def setup() {
        repose.applyConfigs( "features/core/powerfilter/common" )
        repose.enableJmx()
        repose.start()

        deproxy.addEndpoint(properties.getProperty("target.p"))
    }

    def cleanup() {
        repose.stop()
    }

    def "when sending requests, response code counters should be incremented"() {

        given:
        def expectedCount = 4

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.makeRequest( "/endpoint" );
        deproxy.makeRequest( "/endpoint" );
        deproxy.makeRequest( "/cluster" );

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
        reposeCount == expectedCount
    }


    def "when sending requests to service cluster, response codes should be recorded"() {

        when:

        // NOTE:  We verify that Repose is up and running by sending a GET request in repose.start()
        // This is logged as well, so we need to add this to our count

        deproxy.doGet( "/endpoint", new HashMap() );
        deproxy.doGet( "/endpoint", new HashMap() );
        deproxy.doGet( "/cluster", new HashMap() );

        def reposeCount = repose.jmx.getMBeanAttribute( reposeName, "Count" )

        // will need to pull the IP on the machine running deproxy as this is used
        // in the bean name


        /**
         *  TODO:
         *
         *  1) Return counts for different types of return codes
         *  2) Need Deproxy is create endpoint service
         *  3) Need to verify counts for:
         *     - endpoint
         *     - cluster
         *     - All Endpoints
         *  4) Need to verify that Repose 5XX is sum of all non "All Endpoints" 5XX bean
         *  5) Need to verify that Repose 2XX is sum of all non "All Endpoints" 2XX bean
         */

        then:
        reposeCount == 4


    }

    def "when responses with different status codes, should be grouped by status family"(){}

}
