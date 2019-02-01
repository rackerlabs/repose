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
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters

/**
 * Trying to reduce some of the copypasta in all the rate limiting tests :|
 */
@Category(Filters)
class RateLimitMeasurementUtilities {

    private Deproxy deproxy
    private Map<String, String> groupHeaderDefault
    private Map<String, String> userHeaderDefault
    private String reposeEndpoint
    //This guy can only handle JSONs so that's all you get
    final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]


    public RateLimitMeasurementUtilities(Deproxy deproxy,
                                         String reposeEndpoint,
                                         Map<String, String> groupHeaderDefault,
                                         Map<String, String> userHeaderDefault
    ) {
        this.deproxy = deproxy
        this.groupHeaderDefault = groupHeaderDefault
        this.userHeaderDefault = userHeaderDefault
        this.reposeEndpoint = reposeEndpoint
    }

    private static int parseAbsoluteLimitFromJSON(String body, int limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate[limit].limit[0].value
    }

    //using this for now
    private static int parseRemainingFromJSON(String body, int limit) {
        def json = JsonSlurper.newInstance().parseText(body)
        return json.limits.rate[limit].limit[0].remaining
    }

    private String getDefaultLimits(Map group = null) {
        def groupHeader = (group != null) ? group : groupHeaderDefault
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + groupHeader + acceptHeaderJson);

        return messageChain.receivedResponse.body
    }

    /**
     * Get the specific limits for a user
     * @param headers
     * @return
     */
    public String getSpecificUserLimits(Map headers) {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: headers + acceptHeaderJson);

        return messageChain.receivedResponse.body
    }

    /**
     * Pass a header map you wish to mach on to wait for a limit to finish
     * @param group
     */
    public void waitForLimitReset(Map group = null) {
        def ready = false
        while (!ready) {
            def remaining = parseRemainingFromJSON(getDefaultLimits(group), 0)
            def absolute = parseAbsoluteLimitFromJSON(getDefaultLimits(group), 0)
            ready = remaining == absolute

            println("WAITING FOR LIMIT RESET FOR GROUP: ${group}")
            println("REMAINING: ${remaining}")
            println("ABSOLUTE:  ${absolute}")
            sleep(1000)
        }
    }

    /**
     * Consume all the requests for
     * @param user
     * @param group
     * @param path
     */
    public void useAllRemainingRequests(String user, String group, String path) {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                headers: ["X-PP-User": user, "X-PP-Groups": group]);

        while (!messageChain.receivedResponse.code.equals("413")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: "GET",
                    headers: ["X-PP-User": user, "X-PP-Groups": group]);
        }
    }

    /**
     * A utility method when given a list of maps to validate against the json coming back from a get Limits request
     * It will validate that all the check limits, and only the checklimits are returned.
     * @param json
     * @param checklimit
     * @return
     */
    static boolean checkAbsoluteLimitJsonResponse(Map json, List checklimit) {

        def listnode = json.limits.rate["limit"].flatten()
        //Have to massage away the "next-available" from the listnode list
        listnode = listnode.collect { entry ->
            entry.remove("next-available")
            entry
        }

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
}
