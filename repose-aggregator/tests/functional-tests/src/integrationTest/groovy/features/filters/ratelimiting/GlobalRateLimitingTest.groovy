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
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

/**
 * Created by jennyvo on 7/30/14.
 */
@Category(Filters)
class GlobalRateLimitingTest extends ReposeValveTest {
    final handler = { return new Response(200, "OK") }

    final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
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
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/oneNode", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/globalratelimit", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "When Repose config with Global Rate Limit, user limit should hit first"() {
        given: "the rate-limit has not been reached"
        def methods = ["GET", "HEAD", "POST", "PUT", "DELETE", "PATCH"]

        (1..5).each {
            i ->
                when: "the user sends their request and the rate-limit has not been reached"
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: methods[i],
                        headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertThat(messageChain.receivedResponse.code, equalTo("200"))
                assertThat(messageChain.handlings.size(), equalTo(1))
        }

        when: "the user hit the rate-limit"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is rate-limited, and respond with correct respcode"
        messageChain.receivedResponse.code.equals("503")
    }

    def "When Run with different users, hit the same resource, global limit share between users"() {
        given: "the rate-limit has not been reached"
        //waitForLimitReset
        sleep(60000)
        def group = "customer"
        def headers1 = ['X-PP-User': "user1", 'X-PP-Groups': group]
        def headers2 = ['X-PP-User': "user2", 'X-PP-Groups': group]

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
                MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                        headers: headers2, defaultHandler: handler)

                then: "the request is not rate-limited, and passes to the origin service"
                assertThat(messageChain.receivedResponse.code, equalTo("200"))
                assertThat(messageChain.handlings.size(), equalTo(1))
        }

        when: "user1 hit the same resource, rate limitted"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: headers1, defaultHandler: handler)

        then: "the request is rate-limited, and respond with correct respcode"
        messageChain.receivedResponse.code.equals("503")

        when: "user2 hit the same resource, rate limitted"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: headers2, defaultHandler: handler)

        then: "the request is rate-limited, and respond with correct respcode"
        messageChain.receivedResponse.code.equals("503")
    }

    @Unroll("send req with #url, #user, #group, #method, expect #responseCode")
    def "All requests should be limited by the global limit, regardless of #user or #group."() {
        given:
        def response
        def headers = ['X-PP-User': user, 'X-PP-Groups': group]

        when: "we make multiple requests"
        response = deproxy.makeRequest(method: method, url: reposeEndpoint + url, headers: headers)

        then: "it should limit based off of the global rate limit group"
        response.receivedResponse.code == responseCode

        where:
        url      | user    | group    | method  | responseCode
        "/test1" | "user1" | "group1" | "GET"   | "200"
        "/test1" | "user1" | "group1" | "GET"   | "200"
        "/test1" | "user1" | "group1" | "GET"   | "200"
        "/test1" | "user1" | "group1" | "GET"   | "503"
        "/test1" | "user2" | "group1" | "GET"   | "503"
        "/test1" | "user3" | "group2" | "GET"   | "503"
        "/test1" | "user1" | "group1" | "POST"  | "503"
        "/test1" | "user2" | "group1" | "POST"  | "503"
        "/test1" | "user3" | "group2" | "POST"  | "503"

        "/test2" | "user1" | "group1" | "GET"   | "200"
        "/test2" | "user2" | "group2" | "POST"  | "200"
        "/test2" | "user3" | "group3" | "PUT"   | "200"
        "/test2" | "user4" | "group4" | "PATCH" | "503"

        "/test3" | "user1" | "group1" | "POST"  | "200"
        "/test3" | "user1" | "group1" | "PATCH" | "200"
        "/test3" | "user2" | "group2" | "PUT"   | "200"
        "/test3" | "user3" | "group3" | "POST"  | "200"
        "/test3" | "user3" | "group3" | "POST"  | "200"
        "/test3" | "user4" | "group4" | "GET"   | "200"
        "/test3" | "user3" | "group3" | "GET"   | "200"
        "/test3" | "user2" | "group2" | "GET"   | "200"
        "/test3" | "user1" | "group1" | "GET"   | "503"
    }
}
