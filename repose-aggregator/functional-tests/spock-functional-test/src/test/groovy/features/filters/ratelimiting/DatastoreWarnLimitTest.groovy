package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/* Checks to see if DatastoreWarnLimit throws warn in log if hit that limit of cache keys */

class DatastoreWarnLimitTest extends ReposeValveTest{
    static int WARN_LIMIT = 1

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/datastore", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "when sending requests that match capture group with different cache keys should warn when exceeds limit"() {

        given:
        def user= UUID.randomUUID().toString();

        when:
        for (int i = 0; i < totalRequests; i++) {
            def path= UUID.randomUUID().toString();
            deproxy.makeRequest(url:reposeEndpoint+ "/" + path, method:'GET', headers:['X-PP-USER': user, 'X-PP-Groups' : "BETA_Group"])
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
