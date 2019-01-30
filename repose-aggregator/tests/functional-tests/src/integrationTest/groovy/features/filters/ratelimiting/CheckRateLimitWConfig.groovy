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

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class CheckRateLimitWConfig extends ReposeValveTest {
    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/checkratelimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("Check absolute and remaining limit for limit group #limitgroup ")
    def "Check absolute limit on json"() {
        when: "the user send request to get rate limit with endpoint doesn't match with limit group"
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + limitgroup + acceptHeaderJson);
        def jsonbody = mc1.receivedResponse.body.toString()
        def json = JsonSlurper.newInstance().parseText(jsonbody)
        println(jsonbody)

        then:
        mc1.handlings.size() == 1
        RateLimitMeasurementUtilities.checkAbsoluteLimitJsonResponse(json, checklimit)
        mc1.receivedResponse.headers.findAll('Content-Type')

        where:
        limitgroup                          | checklimit
        ["X-PP-Groups": "customer"]         | customerlimit
        ["X-PP-Groups": "higher"]           | highlimit
        ["X-PP-Groups": "reset-limits"]     | resetlimit
        ["X-PP-Groups": "unique"]           | uniquelimit
        ["X-PP-Groups": "multiregex"]       | multiregexlimit
        ["X-PP-Groups": "all-limits"]       | alllimit
        ["X-PP-Groups": "all-limits-small"] | allsmalllimit
        ["X-PP-Groups": "multi-limits"]     | multilimit
        ["X-PP-Groups": "query-limits"]     | querylimit
        ["X-PP-Groups": "unlimited"]        | unlimitedlimit
        ["X-PP-Groups": "user"]             | defaultlimit
    }

    // Describe the limits from limitgroups in the config
    //The ordering on these is totally bogus. There's no way this can work reliably :(
    final static List<Map> customerlimit = [
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'DAY', 'remaining': 5, 'verb': 'DELETE', 'value': 5],
            ['unit': 'MINUTE', 'remaining': 1000, 'verb': 'GET', 'value': 1000],
            ['unit': 'HOUR', 'remaining': 10, 'verb': 'POST', 'value': 10],
            ['unit': 'DAY', 'remaining': 5, 'verb': 'PUT', 'value': 5],
    ]

    final static List<Map> highlimit = [
            ['unit': 'HOUR', 'remaining': 100, 'verb': 'POST', 'value': 100],
            ['unit': 'MINUTE', 'remaining': 30, 'verb': 'GET', 'value': 30],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'PUT', 'value': 50],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'DELETE', 'value': 50]
    ]

    final static List<Map> resetlimit = [
            ['unit': 'MINUTE', 'remaining': 1000, 'verb': 'GET', 'value': 1000],
            ['unit': 'DAY', 'remaining': 5, 'verb': 'PUT', 'value': 5],
            ['unit': 'SECOND', 'remaining': 5, 'verb': 'GET', 'value': 5]
    ]

    final static List<Map> uniquelimit = [
            ['unit': 'HOUR', 'remaining': 100, 'verb': 'POST', 'value': 100],
            ['unit': 'MINUTE', 'remaining': 30, 'verb': 'GET', 'value': 30],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'PUT', 'value': 50],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'DELETE', 'value': 50]
    ]

    // Re ordering to match the config, even though that's not the order repose might return it in, it's better for
    // my sanity
    final static List<Map> multiregexlimit = [
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'POST', 'value': 3],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'HOUR', 'remaining': 100, 'verb': 'POST', 'value': 100],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'PUT', 'value': 50],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'DELETE', 'value': 50]
    ]

    final static List<Map> alllimit = [
            ['unit': 'HOUR', 'remaining': 50, 'verb': 'ALL', 'value': 50]
    ]

    final static List<Map> allsmalllimit = [
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'ALL', 'value': 3]
    ]
    final static List<Map> multilimit = [
            ['unit': 'HOUR', 'remaining': 1, 'verb': 'GET', 'value': 1],
            ['unit': 'HOUR', 'remaining': 1, 'verb': 'POST', 'value': 1]
    ]
    final static List<Map> querylimit = [
            ['unit': 'HOUR', 'remaining': 0, 'verb': 'GET', 'value': 1],
    ]

    //The limits for unlimited should be EMPTY, no limits for this guy
    final static List<Map> unlimitedlimit = []

    final static List<Map> defaultlimit = [
            ['unit': 'MINUTE', 'remaining': 1000, 'verb': 'GET', 'value': 1000],
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'GET', 'value': 3],
            ['unit': 'HOUR', 'remaining': 10, 'verb': 'POST', 'value': 10],
            ['unit': 'DAY', 'remaining': 5, 'verb': 'PUT', 'value': 5],
            ['unit': 'DAY', 'remaining': 5, 'verb': 'DELETE', 'value': 5]
    ]
}

