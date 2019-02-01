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

/**
 * This test provides proof that the fix for REP-2233 works even with multiple filters.
 */
@Category(Filters)
class RateLimitingConflictingIDsMultiFilterTest extends ReposeValveTest {
    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/idconflict", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "Verify that we don't clobber IDs when multiple rate limiting filters are used"() {
        when: "I send a request through repose that has a conflicting Limit ID in a separate filter file"
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "higher"] + acceptHeaderJson);
        def json = JsonSlurper.newInstance().parseText(mc1.receivedResponse.body.toString())

        then: "the limit reporting for the first filter is accurate"
        mc1.handlings.size() == 1
        RateLimitMeasurementUtilities.checkAbsoluteLimitJsonResponse(json, highlimit)

        when: "I send a request through repose to get the limits for the other filter"
        MessageChain mc2 = deproxy.makeRequest(url: reposeEndpoint + "/rate2/limits", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "higher2"] + acceptHeaderJson);
        def json2 = JsonSlurper.newInstance().parseText(mc2.receivedResponse.body.toString())

        then:
        mc2.handlings.size() == 1
        RateLimitMeasurementUtilities.checkAbsoluteLimitJsonResponse(json2, secondFilterGroup)
    }

    // Describe the limits from limitgroups in the configs
    final static List<Map> highlimit = [
            ['unit': 'HOUR', 'remaining': 100, 'verb': 'POST', 'value': 100],
            ['unit': 'MINUTE', 'remaining': 30, 'verb': 'GET', 'value': 30],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'PUT', 'value': 50],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'DELETE', 'value': 50]
    ]

    final static List<Map> secondFilterGroup = [
            ['unit': 'MINUTE', 'remaining': 30, 'verb': 'GET', 'value': 30],
            ['unit': 'HOUR', 'remaining': 100, 'verb': 'POST', 'value': 100],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'PUT', 'value': 50],
            ['unit': 'DAY', 'remaining': 50, 'verb': 'DELETE', 'value': 50]
    ]
}

