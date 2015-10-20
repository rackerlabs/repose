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

import framework.ReposeValveTest
import groovy.json.JsonSlurper
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class RateLimitingConflictingIDsMultiFilterTest extends ReposeValveTest {
    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/idconflict", params)
        repose.start()
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    //2233
    def "Verify that we don't clobber IDs when multiple rate limiting filters are used"() {
        when: "I send a request through repose that has a conflicting Limit ID in a separate filter file"
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "higher"] + acceptHeaderJson);
        def json = JsonSlurper.newInstance().parseText(mc1.receivedResponse.body.toString())

        then: "the limit reporting for the first filter is accurate"
        mc1.handlings.size() == 1
        checkAbsoluteLimitJsonResponse(json, highlimit)

        when: "I send a request through repose to get the limits for the other filter"
        MessageChain mc2 = deproxy.makeRequest(url: reposeEndpoint + "/rate2/limits", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "higher2"] + acceptHeaderJson);
        def json2 = JsonSlurper.newInstance().parseText(mc2.receivedResponse.body.toString())

        then:
        mc2.handlings.size() == 1
        checkAbsoluteLimitJsonResponse(json2, secondFilterGroup)
    }

    //Just doing the assertions provides a much better output from spock
    static boolean checkAbsoluteLimitJsonResponse(Map json, List checklimit) {

        def listnode = json.limits.rate["limit"].flatten()
        //Have to massage away the "next-available" from the listnode list
        listnode = listnode.collect { entry ->
            entry.remove("next-available")
            entry
        }
        println("LISTNODE:   ${listnode}")
        println("CHECKLIMIT: ${checklimit}")

        //Subtract the required checks from the results on repose
        // If the list is empty, then we checked *everything* and didn't get any other limits back
        // If it's nonempty, we got other limits back that we didn't check for
        def onlyAllChecksFound = listnode - checklimit
        assert onlyAllChecksFound.size() == 0

        //Subtract the result from repose from our checks
        // If the result is an empty list, then all the checks were found!
        def allChecksFound = checklimit - listnode
        assert allChecksFound.size() == 0

        return true
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

