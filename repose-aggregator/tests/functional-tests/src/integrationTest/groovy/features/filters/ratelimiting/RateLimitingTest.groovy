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
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Header
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Shared
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*
import static javax.ws.rs.core.HttpHeaders.ACCEPT
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.*
import static org.openrepose.commons.utils.http.normal.ExtendedStatusCodes.SC_TOO_MANY_REQUESTS

/**
 * Rate limiting tests ported over from python and JMeter
 *  update test to get limits response in json to parse response and calculate
 *  since often get inconsistent xml response fro limits cause Rate Limiting Tests flaky.
 */
class RateLimitingTest extends ReposeValveTest {
    final handler = { return new Response(SC_OK, "OK") }

    static final Map<String, String> userHeaderDefault = ["X-PP-User": "user"]
    static final Map<String, String> groupHeaderDefault = ["X-PP-Groups": "customer"]
    static final Map<String, String> acceptHeaderJson = ["Accept": "application/json"]
    static final String LIMITS_URL = "/service2/limits"
    static final String APPLICATION_WILDCARD = "application/$MEDIA_TYPE_WILDCARD"
    static final String AUDIO_WILDCARD = "audio/$MEDIA_TYPE_WILDCARD"

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
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "PUT",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1

        when: "the user sends their request and the rate-limit has not been reached"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        messageChain.handlings.size() == 0
    }


    def "When a limit has not been reached, request should pass"() {
        given: "the rate-limit has not been reached"
        // A new user is used to prevent waiting for rate limit to reset

        when: "the user sends their request and the rate-limit has not been reached"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint, method: "GET",
                headers: ["X-PP-User": "tester"] + groupHeaderDefault, defaultHandler: handler)

        then: "the request is not rate-limited, and passes to the origin service"
        messageChain.receivedResponse.code as Integer == SC_OK
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
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
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
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
        messageChain.handlings.size() == 0

        when: "a minute passes, and another request is sent"
        sleep(60000)
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "rate limit should have reset, and request should succeed"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1
    }

    def "When rate limiting requests with multiple X-PP-User values, should allow requests with new username"() {
        given: "the limit has been reached for the default user"
        rlmu.useAllRemainingRequests("user", "all-limits-small", "/service/limits")

        when: "a request is made by a different user"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/limits", method: "GET",
                headers: ["X-PP-User": "that_other_user;q=1.0"] + ['X-PP-Groups': 'all-limits-small'], defaultHandler: handler)

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK
    }

    def "When rate limiting requests with multiple X-PP-Group values, should allow requests with new group with higher priority"() {
        given: "the limit has been reached for a user in a certain group"
        for (x in 0..3) {
            deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                    headers: userHeaderDefault + ["X-PP-Groups": "customer"])

        }
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer"])

        then:
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        when: "a request is made using a new group with a higher quality"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer;q=0.5,higher;q=0.75"])

        then: "the request should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "a request is made using a new group with a higher and lower quality"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "GET",
                headers: userHeaderDefault + ["X-PP-Groups": "customer;q=0.5,high;q=0.75,lower;q=0.0,higher;q=0.9,other;q=0.6,none"])

        then: "the request should be rate limited"
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
    }

    def "When requesting rate limits with an invalid Accept header, Should receive 406 response when invalid Accept header"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + LIMITS_URL, method: "GET",
                headers: ["X-PP-Groups": "customer;q=1.0", "X-PP-User": "user", "Accept": "application/unknown"])

        then:
        messageChain.receivedResponse.code as Integer == SC_NOT_ACCEPTABLE
    }

    @Unroll
    def "When requesting rate limits with the Accept header values '#acceptHeaderValues', the response Content-Type should be '#expectedContentType'"() {
        given:
        def headers = [
                new Header("X-PP-Groups", "customer;q=1.0"),
                new Header("X-PP-User", "user")
        ] + acceptHeaderValues.collect { new Header(ACCEPT, it) }

        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint + LIMITS_URL,
                method: "GET",
                headers: headers)

        then:
        messageChain.receivedResponse.headers.getFirstValue(CONTENT_TYPE) == expectedContentType

        where:
        expectedContentType | acceptHeaderValues
        // no/empty Accept header - defaults to JSON
        APPLICATION_JSON    | []
        APPLICATION_JSON    | [""]
        // uses specified Accept header
        APPLICATION_JSON    | [APPLICATION_JSON]
        APPLICATION_XML     | [APPLICATION_XML]
        // is okay with having a quality value
        APPLICATION_JSON    | ["$APPLICATION_JSON;q0.6"]
        APPLICATION_XML     | ["$APPLICATION_XML;q0.5"]
        // uses the quality value (single string of header values)
        APPLICATION_JSON    | ["$APPLICATION_JSON;q0.9,$APPLICATION_XML;q=0.8"]
        APPLICATION_XML     | ["$APPLICATION_XML;q0.7,$APPLICATION_JSON;q=0.6"]
        // uses the quality value (multiple strings of header values)
        APPLICATION_JSON    | ["$APPLICATION_JSON;q0.9", "$APPLICATION_XML;q=0.8"]
        APPLICATION_XML     | ["$APPLICATION_XML;q0.7", "$APPLICATION_JSON;q=0.6"]
        // correctly defaults no quality value to 1.0
        APPLICATION_JSON    | ["$APPLICATION_XML;q=0.8,$APPLICATION_JSON"]
        APPLICATION_JSON    | ["$APPLICATION_XML;q=0.8", APPLICATION_JSON]
        APPLICATION_XML     | ["$APPLICATION_JSON;q=0.6,$APPLICATION_XML"]
        APPLICATION_XML     | ["$APPLICATION_JSON;q=0.6", APPLICATION_XML]
        // ignores unsupported value when a supported value is available (same quality)
        APPLICATION_JSON    | ["$TEXT_XML,$APPLICATION_JSON"]
        APPLICATION_JSON    | [TEXT_XML, APPLICATION_JSON]
        APPLICATION_XML     | ["stone/hieroglyphs,$APPLICATION_XML"]
        APPLICATION_XML     | ["stone/hieroglyphs", APPLICATION_XML]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_JSON,$TEXT_XML"]
        APPLICATION_JSON    | [APPLICATION_JSON, TEXT_XML]
        APPLICATION_XML     | ["$APPLICATION_XML,stone/hieroglyphs"]
        APPLICATION_XML     | [APPLICATION_XML, "stone/hieroglyphs"]
        // ignores unsupported value when a supported value is available (lower quality), also okay with 0.001 value
        APPLICATION_JSON    | ["$TEXT_XML,$APPLICATION_JSON;q=0.001"]
        APPLICATION_JSON    | [TEXT_XML, "$APPLICATION_JSON;q=0.001"]
        APPLICATION_XML     | ["stone/hieroglyphs,$APPLICATION_XML;q=0.001"]
        APPLICATION_XML     | ["stone/hieroglyphs", "$APPLICATION_XML;q=0.001"]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_JSON;q=0.001,$TEXT_XML"]
        APPLICATION_JSON    | ["$APPLICATION_JSON;q=0.001", TEXT_XML]
        APPLICATION_XML     | ["$APPLICATION_XML;q=0.001,stone/hieroglyphs"]
        APPLICATION_XML     | ["$APPLICATION_XML;q=0.001", "stone/hieroglyphs"]
        // is okay with lots of values
        APPLICATION_JSON    | ["a/b,c/d,e/f,$APPLICATION_JSON,$TEXT_XML,stone/hieroglyphs,parrot/caw,image/gif"]
        APPLICATION_XML     | ["a/b,c/d,e/f,$APPLICATION_XML,$TEXT_XML,stone/hieroglyphs,parrot/caw,image/gif"]
        // wildcards
        APPLICATION_JSON    | [WILDCARD]
        APPLICATION_JSON    | [APPLICATION_WILDCARD]
        // list of wildcards
        APPLICATION_JSON    | ["$APPLICATION_WILDCARD,$WILDCARD"]
        APPLICATION_JSON    | [APPLICATION_WILDCARD, WILDCARD]
        APPLICATION_JSON    | ["$WILDCARD,$APPLICATION_WILDCARD"]
        APPLICATION_JSON    | [WILDCARD, APPLICATION_WILDCARD]
        // media range should be overridden by more specified media type (two layers)
        APPLICATION_JSON    | ["$APPLICATION_WILDCARD,$APPLICATION_JSON"]
        APPLICATION_JSON    | [APPLICATION_WILDCARD, APPLICATION_JSON]
        APPLICATION_XML     | ["$APPLICATION_WILDCARD,$APPLICATION_XML"]
        APPLICATION_XML     | [APPLICATION_WILDCARD, APPLICATION_XML]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_JSON,$APPLICATION_WILDCARD"]
        APPLICATION_JSON    | [APPLICATION_JSON, APPLICATION_WILDCARD]
        APPLICATION_XML     | ["$APPLICATION_XML,$APPLICATION_WILDCARD"]
        APPLICATION_XML     | [APPLICATION_XML, APPLICATION_WILDCARD]
        // media range should be overridden by more specified media type (three layers)
        APPLICATION_JSON    | ["$WILDCARD,$APPLICATION_WILDCARD,$APPLICATION_JSON"]
        APPLICATION_JSON    | [WILDCARD, APPLICATION_WILDCARD, APPLICATION_JSON]
        APPLICATION_XML     | ["$WILDCARD,$APPLICATION_WILDCARD,$APPLICATION_XML"]
        APPLICATION_XML     | [WILDCARD, APPLICATION_WILDCARD, APPLICATION_XML]
        // same as previous but with most specific media type in the middle
        APPLICATION_JSON    | ["$WILDCARD,$APPLICATION_JSON,$APPLICATION_WILDCARD"]
        APPLICATION_JSON    | [WILDCARD, APPLICATION_JSON, APPLICATION_WILDCARD]
        APPLICATION_XML     | ["$WILDCARD,$APPLICATION_XML,$APPLICATION_WILDCARD"]
        APPLICATION_XML     | [WILDCARD, APPLICATION_XML, APPLICATION_WILDCARD]
        // same as previous but with most specific media type in the beginning
        APPLICATION_JSON    | ["$APPLICATION_JSON,$WILDCARD,$APPLICATION_WILDCARD"]
        APPLICATION_JSON    | [APPLICATION_JSON, WILDCARD, APPLICATION_WILDCARD]
        APPLICATION_XML     | ["$APPLICATION_XML,$WILDCARD,$APPLICATION_WILDCARD"]
        APPLICATION_XML     | [APPLICATION_XML, WILDCARD, APPLICATION_WILDCARD]
        // quality should be considered before media type specificity
        APPLICATION_JSON    | ["$APPLICATION_XML;q=0.4,$APPLICATION_WILDCARD;q=0.7"]
        APPLICATION_JSON    | ["$APPLICATION_XML;q=0.4", "$APPLICATION_WILDCARD;q=0.7"]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_WILDCARD;q=0.7,$APPLICATION_XML;q=0.4"]
        APPLICATION_JSON    | ["$APPLICATION_WILDCARD;q=0.7", "$APPLICATION_XML;q=0.4"]
        // wildcards for unsupported media range with a supported media type
        APPLICATION_JSON    | ["$AUDIO_WILDCARD,$APPLICATION_JSON"]
        APPLICATION_JSON    | [AUDIO_WILDCARD, APPLICATION_JSON]
        APPLICATION_XML     | ["$AUDIO_WILDCARD,$APPLICATION_XML"]
        APPLICATION_XML     | [AUDIO_WILDCARD, APPLICATION_XML]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_JSON,$AUDIO_WILDCARD"]
        APPLICATION_JSON    | [APPLICATION_JSON, AUDIO_WILDCARD]
        APPLICATION_XML     | ["$APPLICATION_XML,$AUDIO_WILDCARD"]
        APPLICATION_XML     | [APPLICATION_XML, AUDIO_WILDCARD]
        // wildcards for unsupported media range with a supported media type at a lower quality
        APPLICATION_JSON    | ["$AUDIO_WILDCARD,$APPLICATION_JSON;q=0.8"]
        APPLICATION_JSON    | [AUDIO_WILDCARD, "$APPLICATION_JSON;q=0.8"]
        APPLICATION_XML     | ["$AUDIO_WILDCARD,$APPLICATION_XML;q=0.2"]
        APPLICATION_XML     | [AUDIO_WILDCARD, "$APPLICATION_XML;q=0.2"]
        // same as previous but with swapped order
        APPLICATION_JSON    | ["$APPLICATION_JSON;q=0.8,$AUDIO_WILDCARD"]
        APPLICATION_JSON    | ["$APPLICATION_JSON;q=0.8", AUDIO_WILDCARD]
        APPLICATION_XML     | ["$APPLICATION_XML;q=0.2,$AUDIO_WILDCARD"]
        APPLICATION_XML     | ["$APPLICATION_XML;q=0.2", AUDIO_WILDCARD]
    }

    @Unroll
    def "When requesting rate limits with the Accept header values '#acceptHeaderValues', the response Content-Type should be application/json or application/xml"() {
        given:
        def headers = [
                new Header("X-PP-Groups", "customer;q=1.0"),
                new Header("X-PP-User", "user")
        ] + acceptHeaderValues.collect { new Header(ACCEPT, it) }

        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint + LIMITS_URL,
                method: "GET",
                headers: headers)

        then: "the response Content-Type is either JSON or XML"
        messageChain.receivedResponse.headers.getFirstValue(CONTENT_TYPE) in [APPLICATION_JSON, APPLICATION_XML]

        where:
        acceptHeaderValues << [
                ["$APPLICATION_JSON,$APPLICATION_XML"],
                [APPLICATION_JSON, APPLICATION_XML],
                ["$APPLICATION_XML,$APPLICATION_JSON"],
                [APPLICATION_XML, APPLICATION_JSON],
                ["$APPLICATION_JSON;q0.7,$APPLICATION_XML;q0.7"],
                ["$APPLICATION_JSON;q0.4", "$APPLICATION_XML;q0.4"],
                ["$APPLICATION_XML;q0.3,$APPLICATION_JSON;q0.3"],
                ["$APPLICATION_XML;q0.02", "$APPLICATION_JSON;q0.02"]]
    }

    @Unroll
    def "When requesting rate limits with Accept values that aren't supported '#acceptHeaderValues', a 406 is returned"() {
        given:
        def headers = [
                new Header("X-PP-Groups", "customer;q=1.0"),
                new Header("X-PP-User", "user")
        ] + acceptHeaderValues.collect { new Header(ACCEPT, it) }

        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint + LIMITS_URL,
                method: "GET",
                headers: headers)

        then: "a 406 is returned"
        messageChain.receivedResponse.code as Integer == SC_NOT_ACCEPTABLE

        and: "the origin service does not receive the request"
        messageChain.handlings.isEmpty()

        where:
        acceptHeaderValues << [
                // unsupported value
                [TEXT_XML],
                ["stone/hieroglyphs"],
                // wildcards with zero quality
                ["$WILDCARD;q=0"],
                ["application/*;q=0"],
                // unsupported wildcard media range
                [AUDIO_WILDCARD],
                // JSON is unacceptable by the client
                ["$APPLICATION_JSON;q=0"],
                ["$APPLICATION_JSON;q=0.0"],
                ["$APPLICATION_JSON;q=0.00"],
                ["$APPLICATION_JSON;q=0.000"],
                // XML is unacceptable by the client
                // the filter intentionally does not default to JSON since an Accept header was specified
                ["$APPLICATION_XML;q=0"],
                ["$APPLICATION_XML;q=0.0"],
                ["$APPLICATION_XML;q=0.00"],
                ["$APPLICATION_XML;q=0.000"],
                // supported value is unacceptable by the client and acceptable value is unsupported
                ["$APPLICATION_JSON;q=0.0,potato/salad"],
                ["$APPLICATION_JSON;q=0.0", "potato/salad"],
                ["$APPLICATION_XML;q=0.0,potato/salad"],
                ["$APPLICATION_XML;q=0.0", "potato/salad"],
                // same as previous but with swapped order
                ["potato/salad,$APPLICATION_JSON;q=0.0"],
                ["potato/salad", "$APPLICATION_JSON;q=0.0"],
                ["potato/salad,$APPLICATION_XML;q=0.0"],
                ["potato/salad", "$APPLICATION_XML;q=0.0"],
                // supported value is unacceptable by the client and wildcard value is unsupported
                ["potato/salad,$WILDCARD;q=0.0"],
                ["potato/salad", "$WILDCARD;q=0.0"],
                // same as previous but with swapped order
                ["$WILDCARD;q=0.0,potato/salad"],
                ["$WILDCARD;q=0.0", "potato/salad"]]
    }

    def "When rate limiting against multiple regexes, Should not limit requests against a different regex"() {
        given:
        rlmu.useAllRemainingRequests("user", "multiregex", "/service/endpoint1")

        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/endpoint1", method: "GET",
                headers: ["X-PP-Groups": "multiregex", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/endpoint2", method: "GET",
                headers: ["X-PP-Groups": "multiregex", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code as Integer == SC_OK
    }

    def "When rate limiting against ALL HTTP methods, should"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/all", method: "POST",
                headers: ["X-PP-Groups": "all-limits", "X-PP-User": "123ALL"])

        then:
        messageChain.receivedResponse.code as Integer == SC_OK

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "DELETE",
                headers: ["X-PP-Groups": "all-limits", "X-PP-User": "user"])

        then:
        messageChain.receivedResponse.code as Integer == SC_OK

    }

    def "When making request against a limit with DAY units after a request against a limit with SECOND units, limits don't get overwritten on expire"() {
        when: "make a request with DAY units"
        deproxy.makeRequest(url: reposeEndpoint + "/service2/makeput", method: "PUT",
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
        deproxy.makeRequest(url: reposeEndpoint + "/service2/doget", method: "GET",
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
        deproxy.makeRequest(url: reposeEndpoint + "/service2/doget", method: "GET",
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
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/service/test", method: "POST",
                headers: ["X-PP-Groups": "multi-limits", "X-PP-User": "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
    }

    def "When rate limiting with 429 response code set"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "GET",
                headers: ["X-PP-Groups": "multi2-limits", "X-PP-User": "user"])

        then: "should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate2/service/all", method: "POST",
                headers: ["X-PP-Groups": "multi2-limits", "X-PP-User": "user"])

        then: "should be rate limited"
        messageChain.receivedResponse.code as Integer == SC_TOO_MANY_REQUESTS
    }

    def "When rate limiting with 429 response code set with capture groups false"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all1", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all2", method: "GET",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "no requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/rate3/service/all", method: "POST",
                headers: ["X-PP-Groups": "multi3-limits", "X-PP-User": "429User"])

        then: "should be rate limited"
        messageChain.receivedResponse.code as Integer == SC_TOO_MANY_REQUESTS
    }

    def "When rate limiting /limits"() {
        when: "requests remain"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + LIMITS_URL, method: "GET",
                headers: ["X-PP-Groups": "query-limits", "X-PP-User": "123limits"] + acceptHeaderJson)

        then: "should not be rate limited"
        messageChain.receivedResponse.code as Integer == SC_OK

        when: "requests remain"
        messageChain = deproxy.makeRequest(url: reposeEndpoint + LIMITS_URL, method: "GET",
                headers: ["X-PP-Groups": "query-limits", "X-PP-User": "123limits"] + acceptHeaderJson)

        then: "should be rate limited"
        messageChain.receivedResponse.code as Integer == SC_REQUEST_ENTITY_TOO_LARGE
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def handler = { request -> return new Response(SC_CREATED, "Created", respHeaders, "") }
        Map<String, String> headers = ["x-pp-user"   : "usertest1, usertest2, usertest3", "X-PP-Groups": "unlimited",
                                       "Content-Type": "application/xml"]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers,
                defaultHandler: handler)

        then:
        mc.receivedResponse.code as Integer == SC_CREATED
        mc.handlings.size() == 1
        mc.receivedResponse.headers.findAll("location").size() == 1
        mc.receivedResponse.headers['location'] == "$reposeEndpoint/blah?a=b,c,d"
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
                method: 'GET', headers: headers, defaultHandler: { new Response(SC_CREATED, "Created", respHeaders, "") })

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
                defaultHandler: { return new Response(SC_MOVED_TEMPORARILY, "Redirect") })

        then: "the response code does not change"
        messageChain.receivedResponse.code as Integer == SC_MOVED_TEMPORARILY
        messageChain.handlings.size() == 1
    }

    def "Check limit group"() {
        given: "The limits have been reset for the request we're about to make"
        rlmu.waitForLimitReset(["X-PP-Groups": "all-limits-small"])

        when: "the user send their request"
        MessageChain messageChain = deproxy.makeRequest(url: reposeEndpoint + LIMITS_URL, method: "GET",
                headers: userHeaderDefault + ['X-PP-Groups': 'all-limits-small'] + acceptHeaderJson)
        def jsonbody = messageChain.receivedResponse.body
        println jsonbody
        def json = JsonSlurper.newInstance().parseText(jsonbody)

        then: "the response code does not change"
        messageChain.receivedResponse.code as Integer == SC_OK
        messageChain.handlings.size() == 1
        rlmu.checkAbsoluteLimitJsonResponse(json, allsmalllimit)
    }

    final static List<Map> allsmalllimit = [
            ['unit': 'MINUTE', 'remaining': 3, 'verb': 'ALL', 'value': 3]
    ]
}
