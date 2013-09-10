package features.filters.normalization.uri

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

/**
 * Functional test for the URI Normalization filter
 */
class UriNormalizationFilterTest extends ReposeValveTest {

    def setupSpec() {
        repose.applyConfigs(
                "features/filters/normalization/uri")
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    @Unroll("URI Normalization of queryParameters should #behaviorExpected")
    def "query parameter normalization"() {
        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith("?" + qpAfterRepose)

        where:
        method   | matchingUriRegex     | qpBeforeRepose                      | qpAfterRepose               | behaviorExpected
        "GET"    | "uri_normalization"  | "filter_me=true&a=1&a=2&a=3"        | "a=1&a=2&a=3"               | "filter out non-whitelisted parameters"
        "GET"    | "uri_normalization"  | "a=1&a=2&a=3&a=4"                   | "a=1&a=2&a=3&a=4"           | "retain all QueryParams with Multiplicity of 0"
        "GET"    | "uri_normalization"  | "a=Add+Space"                       | "a=Add+Space"               | "send URL encoded spaces as '+'"
        "GET"    | "uri_normalization"  | "r=123&r=second&r=2334&r=1&r=2&r=5" | "r=123&r=second&r=2334"     | "retain first 3 QPs when Multiplicity is set to '3'"
        "GET"    | "uri_normalization"  | "a=1&r=1&r=2&N=test"                | "a=1&r=1&r=2"               | "remove QueryParams that don't match based on case sensitive query param name"
        "GET"    | "uri_normalization"  | "a=1&n=test&N=nonmatchingCase"      | "a=1&n=test"                | "remove QueryParams that don't match based on case sensitive query param name"
        "GET"    | "uri_normalization"  | "a=1&filter_me=true"                | "a=1"                       | "apply whitelist due to matching http method"
        "GET"    | "uri_normalization/"  | "a=1&filter_me=true"                | "a=1"
        "POST"   | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
        "PUT"    | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
        "DELETE" | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
    }

   def "query parameter normalization with emty target"
    /Users/kush5342/forked_Repose/repose/test/spock-functional-test/src/test/configs/features/filters/normalization/uri/emtpyuritarget

}