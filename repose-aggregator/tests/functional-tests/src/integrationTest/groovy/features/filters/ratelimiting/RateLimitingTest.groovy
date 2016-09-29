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
import framework.category.Slow
import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Rate limiting tests ported over from python and JMeter
 *  update test to get limits response in json to parse response and calculate
 *  since often get inconsistent xml response fro limits cause Rate Limiting Tests flaky.
 */
class RateLimitingTest extends ReposeValveTest {
    final handler = { return new Response(200, "OK") }

    static final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    static final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    static final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]

    @Shared
    private RateLimitMeasurementUtilities rlmu

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/ratelimiting/onenodes", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        //Set up our rate limit utils
        rlmu = new RateLimitMeasurementUtilities(deproxy, reposeEndpoint, groupHeaderDefault, userHeaderDefault)
    }

    def cleanup() {
        rlmu.waitForLimitReset()
    }

    def "When a limit is tested, method should not make a difference"() {
        given: "the rate-limit has not been reached"
        rlmu.waitForLimitReset()

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "PUT",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0

    }


    def "When a limit has not been reached, request should pass"() {
        given: "the rate-limit has not been reached"
        // A new user is used to prevent waiting for rate limit to reset

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["X-PP-User": "tester"] + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1

        where:
        i << [0..4]
    }

    def "When a limit has been reached, request should not pass"() {
        given: "the rate-limit has been reached"
        rlmu.useAllRemainingRequests("user", "all-limits-small", "/service/limits")

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0
    }

    @Category(Slow.class)
    def "When a limit has been reached, the limit should reset after one minute"() {
        given: "the limit has been reached"
        rlmu.useAllRemainingRequests("user", "all-limits-small", "/service/limits")

        when: "another request is sent"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is rate-limited"
        messageChain.receivedResponse.code.equals("413")
        messageChain.handlings.size() == 0

        when: "a minute passes, and another request is sent"
        sleep(60000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "rate limit should have reset, and request should succeed"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
    }

    def "When rate limiting requests with multiple X-PP-User values, should allow requests with new username"() {
        given: "the limit has been reached for the default user"
        rlmu.useAllRemainingRequests("user", "all-limits-small", "/service/limits")

        when: "a request is made by a different user"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-User": "that_other_user;q=1.0"] + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting requests with multiple X-PP-Group values, should allow requests with new group with higher priority"() {
        given: "the limit has been reached for a user in a certain group"
        MessageChain messageChain = null;

        for (x in 0..3) {
            deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                    headers: userHeaderDefault + ["X-PP-Groups": "customer"])

        }
        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer"])

        then:
        messageChain.receivedResponse.code.equals("413")

        when: "a request is made using a new group with a higher quality"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer;q=0.5,higher;q=0.75"])

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "a request is made using a new group with a higher and lower quality"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer;q=0.5,high;q=0.75,lower;q=0.0,higher;q=0.9,other;q=0.6,none"])

        then: "the request should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When requesting rate limits with an invalid Accept header, Should receive 406 response when invalid Accept header"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "customer;q=1.0", "X-PP-User": "user", "Accept": "application/unknown"])

        then:
        messageChain.receivedResponse.code.equals("406")
    }

    def "When rate limiting against multiple regexes, Should not limit requests against a different regex"() {
        given:
        rlmu.useAllRemainingRequests("user", "multiregex", "/service/endpoint1")

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/endpoint1", method: "GET",
                headers: ["X-PP-Groups": "multiregex", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code.equals("413")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/endpoint2", method: "GET",
                headers: ["X-PP-Groups": "multiregex", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code.equals("200")
    }

    def "When rate limiting against ALL HTTP methods, should"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/all", method: "POST",
                headers: ["X-PP-Groups": "all-limits", "X-PP-User": "123ALL"])

        then:
        messageChain.receivedResponse.code.equals("200")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "DELETE",
                headers: ["X-PP-Groups": "all-limits", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code.equals("200")

    }

    def "When making request against a limit with DAY units after a request against a limit with SECOND units, limits don't get overwritten on expire"() {
        when: "make a request with DAY units"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/makeput", method: "PUT",
                headers: ["X-PP-Groups": "reset-limits", "X-PP-User": "123"])
        def slurper = new JsonSlurper()
        def result = slurper.parseText(rlmu.getSpecificUserLimits(
                ["X-PP-Groups": "reset-limits", "X-PP-User": "123"]
        ))

        then:
        result.limits.rate.each {
            t ->
                if (t.regex == "/service2/makeput") {
                    assert t.limit[0].verb == "PUT"
                    assert t.limit[0].value == 5
                    assert t.limit[0].remaining == 4
                    assert t.limit[0].unit == "DAY"
                }
        }

        when: "make a request with SECOND units"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/doget", method: "GET",
                headers: ["X-PP-Groups": "reset-limits", "X-PP-User": "123"])
        slurper = new JsonSlurper()
        result = slurper.parseText(rlmu.getSpecificUserLimits(
                ["X-PP-Groups": "reset-limits", "X-PP-User": "123"]
        ))

        then:
        result.limits.rate.each {
            t ->
                if (t.regex == "/service2/doget") {
                    assert t.limit[0].verb == "GET"
                    assert t.limit[0].value == 5
                    assert t.limit[0].remaining == 4
                    assert t.limit[0].unit == "SECOND"
                } else if (t.regex == "/service2/makeput") {
                    assert t.limit[0].verb == "PUT"
                    assert t.limit[0].value == 5
                    assert t.limit[0].remaining == 4
                    assert t.limit[0].unit == "DAY"
                }

        }

        when: "wait and make a request with SECOND units again"
        sleep(3000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/doget", method: "GET",
                headers: ["X-PP-Groups": "reset-limits", "X-PP-User": "123"])
        slurper = new JsonSlurper()
        result = slurper.parseText(rlmu.getSpecificUserLimits(
                ["X-PP-Groups": "reset-limits", "X-PP-User": "123"]
        ))

        then:
        result.limits.rate.each {
            t ->
                if (t.regex == "/service2/doget") {
                    assert t.limit[0].verb == "GET"
                    assert t.limit[0].value == 5
                    assert t.limit[0].remaining == 4
                    assert t.limit[0].unit == "SECOND"
                } else if (t.regex == "/service2/makeput") {
                    assert t.limit[0].verb == "PUT"
                    assert t.limit[0].value == 5
                    assert t.limit[0].remaining == 4
                    assert t.limit[0].unit == "DAY"
                }
        }
    }

    def "When rate limiting against multiple http methods in single rate limit line"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: ["X-PP-Groups": "multi-limits", "X-PP-User": "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: ["X-PP-Groups": "multi-limits", "X-PP-User": "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "When rate limiting with 429 response code set"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "GET",
                headers: ["X-PP-Groups": "multi2-limits", "X-PP-User": "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "POST",
                headers: ["X-PP-Groups": "multi2-limits", "X-PP-User": "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("429")
    }

    def "When rate limiting with 429 response code set with capture groups false"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all1", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all2", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "POST",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"])

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("429")
    }

    def "When rate limiting /limits"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "query-limits", "X-PP-User": "123limits"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code.equals("200")

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: ["X-PP-Groups": "query-limits", "X-PP-User": "123limits"] + acceptHeaderJson)

        then: "should be rate limited"
        messageChain.receivedResponse.code.equals("413")
    }

    def "Should split request headers according to rfc by default"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
                [
                        "user-agent" : userAgentValue,
                        "x-pp-user"  : "usertest1, usertest2, usertest3",
                        "accept"     : "application/xml;q=1 , application/json;q=0.5",
                        "x-pp-groups": "unlimited"
                ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(201, "Created", respHeaders, "") }
        Map<String, String> headers = ["x-pp-user"   : "usertest1, usertest2, usertest3", "X-PP-Groups": "unlimited",
                                       "Content-Type": "application/xml"]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers,
                defaultHandler: handler)

        then:
        mc.receivedResponse.code == "201"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "http://somehost.com/blah?a=b,c,d"
        mc.receivedResponse.headers.findAll("via").size() == 1
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0',
                "x-pp-user"     : "usertest1, usertest2, usertest3",
                "x-pp-groups"   : "unlimited"
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue


        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }

    @Unroll("Responses - headers: #headerName with \"#headerValue\" keep its case")
    def "Responses - header keep its case in responses"() {
        given:
        def headers = [
                "x-pp-user"  : "usertest1, usertest2, usertest3",
                "x-pp-groups": "unlimited"
        ]
        when: "make a request with the given header and value"
        def respHeaders = [
                "Content-Length": "0",
                "location"      : "http://somehost.com/blah?a=b,c,d"
        ]
        respHeaders[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint,
                method: 'GET', headers: headers, defaultHandler: { new Response(201, "Created", respHeaders, "") })

        then: "the response should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.receivedResponse.headers.contains(headerName)
        mc.receivedResponse.headers.getFirstValue(headerName) == headerValue


        where:
        headerName     | headerValue
        "Content-Type" | "application/json"
        "CONTENT-Type" | "application/json"
        "Content-TYPE" | "application/json"
        //"content-type" | "application/xMl"
        //"Content-Type" | "APPLICATION/xml"
    }

    def "Origin response code should not change when using rate limiting filter"() {
        given: "the ratelimits haven't been hit"
        rlmu.waitForLimitReset(['X-PP-Groups': 'all-limits-small'])

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'],
                defaultHandler: { return new Response(302, "Redirect") })

        then: "the response code does not change"
        messageChain.receivedResponse.code.equals("302")
        messageChain.handlings.size() == 1
    }

    def "Check limit group"() {
        given: "The limits have been reset for the request we're about to make"
        rlmu.waitForLimitReset(["X-PP-Groups": "all-limits-small"])

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service2/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'] + acceptHeaderJson)
        def jsonbody = messageChain.receivedResponse.body
        println jsonbody
        def json = JsonSlurper.newInstance().parseText(jsonbody)
        def listnode = json.limits.rate["limit"]
        List limitlist = []

        then: "the response code does not change"
        messageChain.receivedResponse.code.equals("200")
        messageChain.handlings.size() == 1
        rlmu.checkAbsoluteLimitJsonResponse(json, allsmalllimit)
    }


    final static List<Map> allsmalllimit = [
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'ALL', 'value': 3]
    ]
}