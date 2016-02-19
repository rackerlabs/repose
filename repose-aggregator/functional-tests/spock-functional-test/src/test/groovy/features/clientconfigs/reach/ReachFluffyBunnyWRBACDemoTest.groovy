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
package features.clientconfigs.reach
import framework.ReposeValveTest
import framework.category.Demo
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

import static org.junit.Assert.assertTrue
/**
 * Created by jennyvo on 2/19/16.
 */
@Category(Demo.class)
class ReachFluffyBunnyWRBACDemoTest extends ReposeValveTest {
    final handler = { return new Response(200, "OK") }

    final Map<String, String> userHeaderDefault = ["X-PP-User": "fluffybunny"]
    final Map<String, String> rolesHeaderDefault = ["X-Roles": "fluffybunny"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept": "application/xml"]

    static int userCount = 0;

    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/clientconfigs/reach/fluffybunny", params)
        repose.configurationProvider.applyConfigs("features/clientconfigs/reach/fluffybunny/rbac", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }
    def "Verify lower bound limit" () {
        when: "the user hit the rate-limit"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "PUT",
                headers: userHeaderDefault+rolesHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited, and respond with correct respcode"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When Repose config with Global Rate Limit, user limit should hit first"() {
        given: "the rate-limit has not been reached"
        (1..2).each {
            i ->
                when: "the user sends their request and the rate-limit has not been reached"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: userHeaderDefault + rolesHeaderDefault + groupHeaderDefault, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertTrue(messageChain.receivedResponse.code.equals("200"))
                assertTrue(messageChain.handlings.size() == 1)
        }

        when: "the user hit the rate-limit"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + rolesHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited, and respond with correct respcode"
        messageChain.receivedResponse.code.equals("413")

        "Global limit not Reach but Methods are forbidden by rbac"
        def methods = ["POST","DELETE","HEAD","PATCH"]
        (1..3).each {
            i ->
                when: "the global limit not reach"
                messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: methods[i],
                        headers: ["x-pp-user":"test1"] + rolesHeaderDefault + groupHeaderDefault, defaultHandler: handler)

                then: "the request is not rate-limited, but block by rbac with 405 code"
                assertTrue(messageChain.receivedResponse.code.equals("405"))
                assertTrue(messageChain.handlings.size() == 0)
        }

    }
}
