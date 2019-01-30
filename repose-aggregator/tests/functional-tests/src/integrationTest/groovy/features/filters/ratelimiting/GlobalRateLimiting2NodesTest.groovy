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
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

/**
 * Created by jennyvo on 7/30/14.
 */
@Category(Filters)
class GlobalRateLimiting2NodesTest extends ReposeValveTest {
    final handler = { return new Response(200, "OK") }

    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    final Map<String, String> acceptHeaderDefault = ["Accept": "application/xml"]

    static int reposePort2
    static int distDatastorePort
    static int distDatastorePort2

    def getReposeEndpoint2() {
        return "http://localhost:${reposePort2}"
    }

    static int userCount = 0;

    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        reposePort2 = PortFinder.instance.getNextOpenPort()
        distDatastorePort = PortFinder.instance.getNextOpenPort()
        distDatastorePort2 = PortFinder.instance.getNextOpenPort()

        def params = properties.getDefaultTemplateParams()
        params += [
                reposePort2       : reposePort2,
                distDatastorePort : distDatastorePort,
                distDatastorePort2: distDatastorePort2
        ]


        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/twonodes", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalratelimit", params)
        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "When Repose is configured with multiple nodes, rate-limiting info should be shared"() {
        given: "load the configs for multiple nodes, and use all remaining requests"
        useAllRemainingRequests()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint2, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler)

        then: "the request is rate-limited, and does not pass to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    def "When Run with different users, hit the same resource, global limit share between users"() {
        given: "the rate-limit has not been reached"
        //waitForLimitReset
        sleep(60000)
        def user1 = getNewUniqueUser()
        def user2 = getNewUniqueUser()
        def group = "customer"
        def headers1 = ['X-PP-User': user1, 'X-PP-Groups': group]
        def headers2 = ['X-PP-User': user2, 'X-PP-Groups': group]

        (1..2).each {
            i ->
                when: "user1 hit the same resource"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: headers1, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertThat(messageChain.receivedResponse.code, equalTo("200"))
                assertThat(messageChain.handlings.size(), equalTo(1))
        }

        (1..3).each {
            i ->
                when: "user2 hit the same resource"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint2 + "/service/test", method: "GET",
                        headers: headers2, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertThat(messageChain.receivedResponse.code, equalTo("200"))
                assertThat(messageChain.handlings.size(), equalTo(1))
        }

        when: "user1 hit the same resource, rate limitted"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint2 + "/service/test", method: "GET",
                headers: headers1, defaultHandler: handler)

        then: "the request is rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("503")

        when: "user2 hit the same resource, rate limitted"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: headers2, defaultHandler: handler)

        then: "the request is rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("503")
    }

    private void useAllRemainingRequests() {
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);

        while (messageChain.receivedResponse.code.equals("200")) {
            messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                    headers: userHeaderDefault + groupHeaderDefault, defaultHandler: handler);
        }
    }
}
