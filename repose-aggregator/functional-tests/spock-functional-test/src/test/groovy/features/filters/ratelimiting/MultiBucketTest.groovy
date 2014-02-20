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

    def "when two <limit> elements overlap, requests to the intersection decrement both counters"() {

        given:
        def user = getNewUniqueUser()
        def group = "overlappingLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413"
    }

    def "when two <limit> elements overlap, requests to the non-overlapping uri's decrement separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "overlappingLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413"
    }

    def "when two <limit> elements are disjoint, requests decrement the counters separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "disjointLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "413"
    }

    def "when one <limit> is a strict subset of the other by uri, both counters get decremented"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByUri"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def subresource = "${resource}/subresource"

        expect:
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "413"
    }

    def "when one <limit> is a strict subset of the other by method, both counters get decremented"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByMethod"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"
    }

    def "when one <limit> is a strict subset of the other by method, both counters get decremented 2"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByMethod"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200"
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413"

        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413"
        deproxy.makeRequest(method: 'PUT', url: resource, headers: headers).receivedResponse.code == "413"
        deproxy.makeRequest(method: 'DELETE', url: resource, headers: headers).receivedResponse.code == "413"

    }
    
    @Ignore("The rate limiting config xsd doesn't yet allow for two limits to have the same URI and methods")
    def "when two <limit> elements have the same uri-regex and method, but different units, then they each get counters that decrement separately"() {

        given:
        def user = getNewUniqueUser()
        def group = "differentUnits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                              //        counts: A  B
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

    def "a sequence of <limit> elements is considered in order. limits after a failing one don't get decremented"() {

        given:
        def user = getNewUniqueUser()
        def group = "differentUnits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def hitBoth = "${reposeEndpoint}/abcdef"
        def skipFirst = "${reposeEndpoint}/xyzdef"

        expect:                                                              //         counts: A  B
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "200"   // 1  1
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "200"   // 2  2
        deproxy.makeRequest(url: hitBoth, headers: headers).receivedResponse.code == "413"   // X  2


        deproxy.makeRequest(url: skipFirst, headers: headers).receivedResponse.code == "200" // -  3
        deproxy.makeRequest(url: skipFirst, headers: headers).receivedResponse.code == "413" // -  X

    }

    def "state machine ssppfnn - separate"() {

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
        def all = "${reposeEndpoint}/1234567"

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

        // we know it's skipping the first two, because they only have a value of 1, and would fail on the second request

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
