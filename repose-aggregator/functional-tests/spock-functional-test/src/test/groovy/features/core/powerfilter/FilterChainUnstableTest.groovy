package features.core.powerfilter

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response

import java.util.concurrent.TimeoutException

/* Checks to see if having Unstable filter chain on startup due to configuration errors will log errors into the log file */

class FilterChainUnstableTest extends ReposeValveTest{
    static int requestCount = 1
    def handler5XX = { request -> return new Response(503, 'SERVICE UNAVAILABLE') }

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/badconfigs/")
        try {
            repose.start()
        } catch(TimeoutException e){

        }

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
        sleep(3000)

    }

    def "when sending requests on failure to startup repose due to bad configurations"() {

        given:
        def List<String> logMatches = reposeLogSearch.searchByString("Failed to startup Repose with your configuration. Please check your configuration files and your artifacts directory. Unable to create filter chain.");
        def existingWarningNumber= logMatches.size()

        when:
        for (int i = 0; i < totalRequests; i++) {
            deproxy.makeRequest( [url: reposeEndpoint + "/limits", defaultHandler: handler5XX] )
        }

        then:

        def List<String> logMatchesAfterRequests = reposeLogSearch.searchByString("Failed to startup Repose with your configuration. Please check your configuration files and your artifacts directory. Unable to create filter chain.");
        logMatchesAfterRequests.size() == (expectedWarnings+existingWarningNumber)

        where:

        totalRequests | expectedWarnings
        requestCount | 1
        requestCount+ 2 | 3

    }




}

