/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.ratelimiting

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

@Category(Filters)
class MultiBucketTest extends ReposeValveTest {
    def logSearch = new ReposeLogSearch(properties.logFile)

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/multibucket", params)
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

        /*
         *
         * We define two limits, each allowing 3 requests per minute. One
         * triggers on all http methods with a uri-regex of "/resource.*". The
         * other triggers on all GET requests to any uri.
         *
         * Each of these limits will match a different subset of all requests.
         * In addition, there is a subset of requests that match both limits.
         * A POST request to /resource will trigger only the first limit, a
         * GET to /item will trigger only the second, and a GET to /resource
         * will trigger both.
         *
         * We send four GET requests to /resource. This should completely
         * exhaust both limits. The first three requests should pass and
         * the fourth should fail.
         *
         * After that, we send one POST request to /resource, which should
         * count against the first limit and fail. Finally, we send a GET
         * request to /item, which should count against the second limit and
         * fail.
         *
         */

        given:
        def user = getNewUniqueUser()
        def group = "overlappingLimits"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def item = "${reposeEndpoint}/item"

        expect:                                                                                     // counts: A B
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 1 1
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 2 2
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 3 3
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413" // X -
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413" // - X
    }

    def "when two <limit> elements overlap, requests to the non-overlapping uri's are counted separately"() {

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

        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200" // - 1
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200" // - 2
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "200" // - 3
        deproxy.makeRequest(method: 'GET', url: item, headers: headers).receivedResponse.code == "413" // - X
    }

    def "when two <limit> elements are disjoint, requests count against the limits separately"() {

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

        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200" // - 1
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200" // - 2
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "200" // - 3
        deproxy.makeRequest(url: item, headers: headers).receivedResponse.code == "413" // - X
    }

    def "when one <limit> is a strict subset of the other by uri, both counters are affected"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByUri"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"
        def subresource = "${resource}/subresource"

        expect:
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 1 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 2 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 3 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 4 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(url: subresource, headers: headers).receivedResponse.code == "413" // X -
    }

    def "when one <limit> is a strict subset of the other by uri, both counters are affected 2"() {

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

        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "200" // 4 -
        deproxy.makeRequest(url: resource, headers: headers).receivedResponse.code == "413" // X -
    }

    def "when one <limit> is a strict subset of the other by method, both counters are affected"() {

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

        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413" // - X
    }

    def "when one <limit> is a strict subset of the other by method, both counters are affected 2"() {

        given:
        def user = getNewUniqueUser()
        def group = "subsetLimitsByMethod"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        def resource = "${reposeEndpoint}/resource"

        expect:                                                                                       // counts: A B
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 1 1
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 2 2
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "200" // 3 3
        deproxy.makeRequest(method: 'GET', url: resource, headers: headers).receivedResponse.code == "413" // X -

        deproxy.makeRequest(method: 'POST', url: resource, headers: headers).receivedResponse.code == "413" // X -
        deproxy.makeRequest(method: 'PUT', url: resource, headers: headers).receivedResponse.code == "413" // X -
        deproxy.makeRequest(method: 'DELETE', url: resource, headers: headers).receivedResponse.code == "413" // X -

    }

    def "when two <limit> elements have the same uri-regex and method, but different units, then they each get counters that counted separately"() {

        /*
         * We define two limits that have the same https-methods and the same
         * uri-regex, but different units and value. Requests to the specified
         * uri should count against both, but they will reset at different rate.
         * The first limit allows 3 requests per second, the second allows 5
         * requests per minute.
         *
         * We send four requests to the uri. The first three should pass, and
         * the fourth should fail, because it exhausts the 3-per-second limit.
         *
         * We then wait for 2 seconds, enough time for the per-second limit to
         * reset, but not enough for the per-minute one to reset.
         *
         * Next, we send three more requests (the fifth, sixth, and seventh in
         * the test as a whole). The fifth and sixth requests should pass,
         * because neither limit has been exhausted. The seventh request
         * should fail, because the perm-minute limit is exhausted.
         *
         * Next, we wait for another 2 seconds for the per-second limit to
         * reset.
         *
         * Finally, we send two more requests which should both fail, because
         * the per-minute limit hasn't yet rest.
         *
         * If the rate limiter incorrectly counts the fourth request against
         * the per-minute limit, then the sixth request will fail.
         */

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

        /*
         * If a limit has an http-methods of "ALL", it should match against
         * any request method. All requests that match should be counted
         * together in the same bucket, rather than having separate buckets
         * for each matching method.
         *
         * We define a limit with an http-method of "ALL" and a value of "8".
         * The ALL will match the following: GET, DELETE, POST, PUT, PATCH,
         * HEAD, OPTIONS, TRACE, and CONNECT. However, CONNECT is a
         * special-purpose method that modifies the connection; it is reserved
         * according to rfc 2616, and will not be tested here.
         *
         * We send one request for each method (except for CONNECT), for a
         * total of eight, and each should pass. Then we send one more
         * request, which should fail.
         *
         * If this test passes, we know that ALL is matching against all eight methods.
         */

        given:
        def user = getNewUniqueUser()
        def group = "allMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "DELETE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "POST", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "PUT", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 4
        deproxy.makeRequest(method: "PATCH", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 5
        deproxy.makeRequest(method: "HEAD", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 6
        deproxy.makeRequest(method: "OPTIONS", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 7
        deproxy.makeRequest(method: "TRACE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 8

        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X
    }

    def "when a <limit> has http-methods of ALL, requests to the same method contribute to the total"() {

        /*
         * If a limit has an http-methods of "ALL", it should match against
         * any request method. All requests that match should be counted
         * together in the same bucket, rather than having separate buckets
         * for each matching method.
         *
         * We define a limit with an http-method of "ALL" and a value of "8".
         * The ALL will match the following: GET, DELETE, POST, PUT, PATCH,
         * HEAD, OPTIONS, TRACE, and CONNECT. However, CONNECT is a
         * special-purpose method that modifies the connection; it is reserved
         * according to rfc 2616, and will not be tested here.
         *
         * We send the same GET request nine times. The first eight requests
         * should exhaust the limit, and the ninth should fail.
         *
         */

        given:
        def user = getNewUniqueUser()
        def group = "allMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 4
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 5
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 6
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 7
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 8
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X
    }

    def "when a <limit> has multiple http-methods, requests to any of the methods go into the same bucket"() {

        given:
        def user = getNewUniqueUser()
        def group = "multipleMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        expect:
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "POST", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "PUT", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X
    }

    def "when a <limit> has specific http-methods, request to other methods are not counted by that <limit>"() {

        given:
        def user = getNewUniqueUser()
        def group = "multipleMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]
        logSearch.cleanLog()

        expect:
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(method: "POST", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(method: "PUT", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(method: "GET", url: reposeEndpoint, headers: headers).receivedResponse.code == "413" // X

        deproxy.makeRequest(method: "DELETE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "PATCH", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "HEAD", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "OPTIONS", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        deproxy.makeRequest(method: "TRACE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -

        //deproxy.makeRequest(method: "SOME",    url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        //deproxy.makeRequest(method: "OTHER",   url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
        //deproxy.makeRequest(method: "GARBAGE", url: reposeEndpoint, headers: headers).receivedResponse.code == "200" // -
    }

    def "when a burst of limits is sent for an execution, only 2x-1 requests can get through"() {

        given:
        def user = getNewUniqueUser()
        def group = "multipleMethods"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        List<Thread> clientThreads = new ArrayList<Thread>()
        def totalSuccessfulCount = 0
        def totalFailedCount = 0
        List<String> requests = new ArrayList()
        int rate_limiting_count = 3

        long start = System.currentTimeMillis()

        for (x in 1..numClients) {

            def thread = Thread.start {
                def threadNum = x

                for (i in 1..callsPerClient) {
                    requests.add('spock-thread-' + threadNum + '-request-' + i)
                    def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers)
                    if (messageChain.receivedResponse.code.equals("200")) {
                        totalSuccessfulCount = totalSuccessfulCount + 1
                        println messageChain.sentRequest.headers
                        println messageChain.sentRequest.body
                        println messageChain.receivedResponse.code
                    } else {
                        totalFailedCount = totalFailedCount + 1
                        println messageChain.receivedResponse.code
                    }

                    Thread.sleep(1000)

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        long stop = System.currentTimeMillis()
        //println totalFailedCount
        //println requests.size()
        assert totalSuccessfulCount < (rate_limiting_count * 2 - 1)

        where:
        numClients | callsPerClient
        1          | 50
    }

    def "counts use a fixed time window"() {

        /*
         * The old model of rate limiting kept track of both individual
         * requests and their timestamps. Thus, when the first request in a
         * sequence "expired", the next request in that sequence now defined
         * the window. In effect, this produced a "sliding window" pattern.
         * That is, when we say that a certain rate limit limits request to,
         * e.g., 3 per minute, then the "minute" that it's talking about would
         * extend from a given request until 60 seconds after it. This ensured
         * that, if we were to choose any arbitrary minute-long section of
         * time, there would be no more than 3 passing requests in that
         * minute-long section.
         *
         *
         *                   X---------|
         *                  X---------|
         *                 X---------|
         *                3---------|
         *            X---------|
         *           3---------|
         *          2---------|
         *    1---------|
         *   -|---------+---------+---------+--------> t
         *    0         1min      2         3
         *         \__________/
         *            3 total
         *
         *
         *
         * In the new model of rate limiting, we use a "fixed window" pattern.
         * The first request in a series of requests defines the start of the
         * window. When that request "expires", then a new request is
         * necessary to start a new window. This is a much simpler pattern to
         * implement, in that we only have to store one timestamp per limit,
         * rather than one per request. However, it changes the effective
         * behavior of limiting when we consider arbitrary sections of time.
         * Each window has a maximum number of passed requests, but if two
         * windows are close together, and the first has almost all of its
         * requests toward the end, and the second has all of its requests
         * toward its start, then there could be a minute-long span of time
         * that contains more passed requests than each individual window is
         * otherwise allowed. The max works out to 2N-1, where N is the max
         * defined fow a single window.
         *
         *
         *    1-----23X-| 123X-----|
         *   -|---------+---------+---------+--------> t
         *    0         1min      2         3
         *         \__________/
         *            5 total
         *
         * This test ensures that repose is tracking requests and times
         * according to the new model. We have a single limit set for 3
         * requests per minute. First, we send a single request (which should
         * pass) and sleep for 57 seconds. This will put the next few requests
         * at the end of the window. After sleeping, we get the current system
         * time.
         *
         * Next, we make three requests (2 thru 4). The second and third
         * requests should pass and the fourth should fail. Then we sleep for
         * another 4 seconds, which should reset the limit (57 + 4 > 60).
         *
         * Then, we make four more requests (5 thru 8). The fifth, sixth, and
         * seventh requests should pass, because the limit reset. The eight
         * should fail.
         *
         * Finally, we get the system time again and check that the difference
         * between the two system times is less than one minute.
         *
         * If Repose is using the old model of rate limiting, then the sixth
         * and seventh requests should fail, because they would be within the
         * one-minute window started by the second request.
         *
         * If the difference between the system times is greater than one
         * minute, then the requests took far longer than we were expecting,
         * and it's possible the limit reset at some point. If that happened,
         * then we can't say for sure that the rate limiter is using the right
         * model.
         *
         * If all assertions pass, then we know that the span of time between
         * the two calls to currentTimeMillis contain 2N - 1 = 5 passed
         * requests, and that that span of time is less than a minute long.
         *
         */

        given:
        def user = getNewUniqueUser()
        def group = "fixedTimeWindow"
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]
        def url = "${reposeEndpoint}/resource"

        expect:                                                                        // count
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 1

        sleep(57000)                                                                   // 1

        def time1 = System.currentTimeMillis()

        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "413" // X

        sleep(4000)                                                                    // 0

        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 1
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 2
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "200" // 3
        deproxy.makeRequest(url: url, headers: headers).receivedResponse.code == "413" // X

        def time2 = System.currentTimeMillis()

        and:
        time2 - time1 < 60000
    }
}
