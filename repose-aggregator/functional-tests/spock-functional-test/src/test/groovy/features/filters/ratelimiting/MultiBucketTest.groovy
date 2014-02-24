package features.filters.ratelimiting

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Ignore

class MultiBucketTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/multiBucket", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

    }

    static int userCount = 0;
    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }

    def "separate users get separate buckets"() {

        given:
        def user1 = getNewUniqueUser()
        def user2 = getNewUniqueUser()
        def group = "limitAll"
        def headers1 = ['X-PP-User': user1, 'X-PP-Groups': group]
        def headers2 = ['X-PP-User': user2, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                                                     //  count: A B
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers1).receivedResponse.code == "200" // 1 -
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers1).receivedResponse.code == "200" // 2 -
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers1).receivedResponse.code == "200" // 3 -
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers1).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers2).receivedResponse.code == "200" // - 1
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers2).receivedResponse.code == "200" // - 2
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers2).receivedResponse.code == "200" // - 3
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers2).receivedResponse.code == "413" // - X
    }

    def "when two <limit> elements overlap, requests to the intersection decrement both counters"() {

        // A - 3 per minute - ALL /resource.*
        // B - 3 per minute - GET /.*
        //
        // a non-GET to /resource.* will only hit A
        // a GET to something other than /resource.* will only hit B
        // a GET to /resource.* will trigger both limits


        given:
        def user = getNewUniqueUser()
        def group = "overlappingLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:                                                                                     // counts: A B
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 1 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 2 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 3 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"  // X 1

        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"      // - 2
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"      // - 3
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413"      // - X
    }

    def "when two <limit> elements overlap, requests to the non-overlapping uri's decrement separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "overlappingLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:                                                                                     // counts: A B
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 1 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 2 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 3 -
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"      // - 1
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"      // - 2
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"      // - 3
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413"      // - X
    }

    def "when two <limit> elements are disjoint, requests decrement the counters separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "disjointLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:                                                                     // counts: A B
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 1 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 2 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 3 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"     // - 1
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"     // - 2
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"     // - 3
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "413"     // - X
    }

    def "when one <limit> is a strict subset of the other by uri, both counters get decremented"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByUri"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def subresource = "${resource}/subresource"

        expect:
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"    // 1 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"    // 2 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"    // 3 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"    // 4 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413"    // X -

        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "413" // X -
    }

    def "when one <limit> is a strict subset of the other by uri, both counters get decremented 2"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByUri"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def subresource = "${resource}/subresource"

        expect:
        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "200" // 1 1
        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "200" // 2 2
        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "413" // 3 X

        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"    // 4 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413"    // X -
    }

    def "when one <limit> is a strict subset of the other by method, both counters get decremented"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByMethod"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                                                     // counts: A B
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 1 1
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 2 2
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200" // 3 3
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"  // - X
    }

    def "when one <limit> is a strict subset of the other by method, both counters get decremented 2"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByMethod"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                                                       // counts: A B
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"    // 1 1
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"    // 2 2
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"    // 3 3
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"    // X -

        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413"   // X -
        deproxy.makeRequest(method: 'PUT', url: resource, headers: headers).receivedResponse.code == "413"    // X -
        deproxy.makeRequest(method: 'DELETE', url: resource, headers: headers).receivedResponse.code == "413" // X -

    }
    
    @Ignore("The rate limiting config xsd doesn't yet allow for two limits to have the same URI and methods")
    def "when two <limit> elements have the same uri-regex and method, but different units, then they each get counters that decrement separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "differentUnits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                                     // counts: A  B
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 1  1
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 2  2
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 3  3
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // X  3
        // 3 per sec exceeded

        sleep 2000 //let the per-second limit reset                                            0  3
        
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 1  4
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 2  5
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // 3  X
        // 5 per minute exceeded

        sleep 2000 //let the per-second limit reset                                            0  X

        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // 1  X
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // 2  X
        
    }

    def "a sequence of <limit> elements is considered in order. limits after a failing one don't get affected"() {

        /*
         * We define two limits. The first allows 2 requests per minute, the
         * second allows 3. The uri-regexes are designed so that we can
         * selectively trigger one limit, or the other, or both, or neither,
         * based on the uri sent.
         *
         * We will send three requests to a uri that triggers both limits. The
         * first two requests should pass successfully. The third should be
         * rejected with a 413, because the first limit only allows 2
         * requests.
         *
         * After those, we will send two more requests (that is, the fourth
         * and fifth of the entire test), this time to a uri that only
         * triggers the second limit. The fourth request should pass, because
         * the second limit has only recorded two requests so far. The fifth
         * request should fail, because it exhausts the second limit.
         *
         * If the rate limiter is incorrectly counting the third request
         * against the second limit, we will know because the fourth request
         * will fail with a 413, instead of passing as it should.
         *
         * If the rate limiter is not counting the first two requests against
         * the second limit, as though it passed the first tests and did not
         * consider the second, we will know because the fifth request will
         * pass and return a 200.
         *
         */

        given:
        def user = getNewUniqueUser()
        def group = "limitsInOrder"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def hitBoth = "${reposeEndpoint}/12"
        def skipFirst = "${reposeEndpoint}/s2"

        expect:                                                                      // counts: A  B
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "200"   // 1  1
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "200"   // 2  2
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "413"   // X  2

        deproxy.makeRequest(url: skipFirst, headers: headers).receivedResponse.code == "200" // -  3
        deproxy.makeRequest(url: skipFirst, headers: headers).receivedResponse.code == "413" // -  X
    }

    def "when a <limit> has http-methods of ALL, requests to any method go into the same bucket"() {

        given:
        def user = getNewUniqueUser()
        def group = "allMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET",     url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "DELETE",  url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "POST",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "PUT",     url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 4
        deproxy.makeRequest(method: "PATCH",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 5
        deproxy.makeRequest(method: "HEAD",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 6
        deproxy.makeRequest(method: "OPTIONS", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 7
        deproxy.makeRequest(method: "CONNECT", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 8
        deproxy.makeRequest(method: "TRACE",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 9

        deproxy.makeRequest(method: "GET",     url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X
    }

    def "when a <limit> has multiple http-methods, requests to any of the methods go into the same bucket"() {

        given:
        def user = getNewUniqueUser()
        def group = "multipleMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET",  url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "POST", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "PUT",  url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "GET",  url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X
    }

    def "when a <limit> has specific http-methods, request to other methods are not counted by that <limit>"() {

        given:
        def user = getNewUniqueUser()
        def group = "multipleMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET",     url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "POST",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "PUT",     url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "GET",     url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X

        deproxy.makeRequest(method: "DELETE",  url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "PATCH",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "HEAD",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "OPTIONS", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "CONNECT", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "TRACE",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -

        deproxy.makeRequest(method: "SOME",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "OTHER",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "GARBAGE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
    }




    def "state machine ssppfnn - separate"() {

        /*
         * The stateMachine-ssppfnn limit group is set up so that we can
         * selectively trigger any subset of the
         *
         *
         */

        given:
        def user = getNewUniqueUser()
        def group = "stateMachine-ssppfnn"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def s1 = "${reposeEndpoint}/1xxxxxx"
        def s2 = "${reposeEndpoint}/x2xxxxx"
        def p3 = "${reposeEndpoint}/xx3xxxx"
        def p4 = "${reposeEndpoint}/xxx4xxx"
        def f5 = "${reposeEndpoint}/xxxx5xx"
        def n6 = "${reposeEndpoint}/xxxxx6x"
        def n7 = "${reposeEndpoint}/xxxxxx7"

        expect:
        deproxy.makeRequest(url: s1, headers: headers).receivedResponse.code == "200"   // 1 - - - - - -
        deproxy.makeRequest(url: s1, headers: headers).receivedResponse.code == "413"   // X - - - - - -

        deproxy.makeRequest(url: s2, headers: headers).receivedResponse.code == "200"   // - 1 - - - - -
        deproxy.makeRequest(url: s2, headers: headers).receivedResponse.code == "413"   // - X - - - - -

        deproxy.makeRequest(url: p3, headers: headers).receivedResponse.code == "200"   // - - 1 - - - -
        deproxy.makeRequest(url: p3, headers: headers).receivedResponse.code == "200"   // - - 2 - - - -
        deproxy.makeRequest(url: p3, headers: headers).receivedResponse.code == "200"   // - - 3 - - - -
        deproxy.makeRequest(url: p3, headers: headers).receivedResponse.code == "413"   // - - X - - - -

        deproxy.makeRequest(url: p4, headers: headers).receivedResponse.code == "200"   // - - - 1 - - -
        deproxy.makeRequest(url: p4, headers: headers).receivedResponse.code == "200"   // - - - 2 - - -
        deproxy.makeRequest(url: p4, headers: headers).receivedResponse.code == "200"   // - - - 3 - - -
        deproxy.makeRequest(url: p4, headers: headers).receivedResponse.code == "413"   // - - - X - - -

        deproxy.makeRequest(url: f5, headers: headers).receivedResponse.code == "200"   // - - - - 1 - -
        deproxy.makeRequest(url: f5, headers: headers).receivedResponse.code == "200"   // - - - - 2 - -
        deproxy.makeRequest(url: f5, headers: headers).receivedResponse.code == "413"   // - - - - X - -

        deproxy.makeRequest(url: n6, headers: headers).receivedResponse.code == "200"   // - - - - - 1 -
        deproxy.makeRequest(url: n6, headers: headers).receivedResponse.code == "200"   // - - - - - 2 -
        deproxy.makeRequest(url: n6, headers: headers).receivedResponse.code == "200"   // - - - - - 3 -
        deproxy.makeRequest(url: n6, headers: headers).receivedResponse.code == "413"   // - - - - - X -

        deproxy.makeRequest(url: n7, headers: headers).receivedResponse.code == "200"   // - - - - - - 1
        deproxy.makeRequest(url: n7, headers: headers).receivedResponse.code == "200"   // - - - - - - 2
        deproxy.makeRequest(url: n7, headers: headers).receivedResponse.code == "200"   // - - - - - - 3
        deproxy.makeRequest(url: n7, headers: headers).receivedResponse.code == "413"   // - - - - - - X

    }

    def "state machine ssppfnn - together"() {

        given:
        def user = getNewUniqueUser()
        def group = "stateMachine-ssppfnn"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def skipFirstTwo = "${reposeEndpoint}/ss34567"

        /*
         * we know it's skipping the first two <limit>'s, because they only
         * allow a single request. If the requests were triggering them, then
         * they would fail on the second request.  before the fifth <limit>.
         */

        expect:
        deproxy.makeRequest(url: skipFirstTwo, headers: headers).receivedResponse.code == "200"   // - - 1 1 1 1 1
        deproxy.makeRequest(url: skipFirstTwo, headers: headers).receivedResponse.code == "200"   // - - 2 2 2 2 2
        deproxy.makeRequest(url: skipFirstTwo, headers: headers).receivedResponse.code == "413"   // - - 3 3 X - -
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
