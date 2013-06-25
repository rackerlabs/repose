package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/* Checks to see if DatastoreWarnLimit throws warn in log if hit that limit of cache keys */

class DatastoreWarnLimitTest extends ReposeValveTest{
    static int WARN_LIMIT = 1

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/ratelimiting/")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
        sleep(3000)

    }

    def "when sending requests that match capture group with different cache keys should warn when exceeds limit"() {

        given:
        def user= UUID.randomUUID().toString();

        when:
        for (int i = 0; i < totalRequests; i++) {
            def path= UUID.randomUUID().toString();
            deproxy.makeRequest(reposeEndpoint+ "/" + path, 'GET', ['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"])
        }

        then:

        def List<String> logMatches = reposeLogSearch.searchByString("Large amount of limits recorded.  Repose Rate Limited may be misconfigured, keeping track of rate limits for user: "+ user +". Please review capture groups in your rate limit configuration.  If using clustered datastore, you may experience network latency.");
        logMatches.size() == expectedWarnings

        where:

        totalRequests  | expectedWarnings
        WARN_LIMIT + 1 | 1
        WARN_LIMIT + 2 | 2

    }




}
