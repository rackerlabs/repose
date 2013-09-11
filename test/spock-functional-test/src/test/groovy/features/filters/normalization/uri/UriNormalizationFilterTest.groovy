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

        "POST"   | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
        "PUT"    | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
        "DELETE" | "uri_normalization"  | "a=1&filter_me=true"                | "a=1&filter_me=true"        | "not apply whitelist when http method does not match"
    }

   @Unroll("URI Normalization of queryParameters #behaviorExpected")
   def "When target is empty in uri filter"(){

       repose.updateConfigs("features/filters/normalization/uri/emtpyuritarget")

        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)

       where:
       method   | matchingUriRegex               | qpBeforeRepose                      | qpAfterRepose                 | behaviorExpected
       "GET"    | "empty_uri_target_with_media"  | "filter_me=true&a=1&a=2&a=3"        | "empty_uri_target_with_media/"  | "Should not contain any query parameters"

   }

    @Unroll("URI Normalization of queryParameters #behaviorExpected")
    def "When http method doesn't match the uri filter"(){

        repose.updateConfigs("features/filters/normalization/uri/withmedia")


        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)


        where:
        method   | matchingUriRegex               | qpBeforeRepose                                              | qpAfterRepose                                               | behaviorExpected
        "POST"    | "uri_normalization_with_media"  | "filter_me=true&a=1&a=4&a=2&r=1241.212&n=test&a=Add+Space"| "filter_me=true&a=1&a=4&a=2&a=Add+Space&r=1241.212&n=test"  | "Should not filter any query parameters"
        "GET"    | "uri_normalization_with_media"  | "a=3&b=4&a=4&A=0&c=6&d=7"                                  | "A=0&a=3&a=4&b=4&c=6"                                       | "Should allow whitelisted query parameters"
        "GET"    | "uri_normalization_with_media"  | "a=3&b=4&a=4&A=0&c=6&d=7&B=8&b=9"                          | "A=0&B=8&a=3&a=4&b=4&c=6"                                   | "Should allow whitelisted query parameters up to multiplicity coun"
        "GET"    | "uri_normalization_with_media"  | "a=3&b=4&a=4&A=0&c=6&C=8&c=10&C=9&c=11"                    | "A=0&a=3&a=4&b=4&c=6&c=10"                                  | "Should allow whitelisted case sensitive query parameters up to multiplicity count"



    }

    @Unroll("URI Normalization of queryParameters #behaviorExpected")
    def "When uri-regex is not specified"(){

        repose.updateConfigs("features/filters/normalization/uri/noregexwithmedia")


        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)

        where:
        method   | matchingUriRegex        | qpBeforeRepose                          | qpAfterRepose                 | behaviorExpected
        "GET"    | "no_regex_with_media"   | "a=3&b=4&a=4&A=0&c=6&C=8&c=10&C=9&c=11" | "A=0&a=3&a=4&b=4&c=6&c=10"   | "Should allow whitelisted case sensitive query parameters up to multiplicity count"

    }

    @Unroll("URI Normalization of queryParameters #behaviorExpected")
    def "When uri filter does not have uri-regex and htt-methods"(){

        repose.updateConfigs("features/filters/normalization/uri/nohttpmethodswithmedia")



        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)

        where:
        method   | matchingUriRegex              | qpBeforeRepose                           | qpAfterRepose               | behaviorExpected
        "GET"    | "no_http_methods_with_media"  | "a=3&b=4&a=4&A=0&c=6&C=8&c=10&C=9&c=11"  | "A=0&a=3&a=4&b=4&c=6&c=10"  | "Should allow whitelisted query parameters"

    }

    @Unroll("URI Normalization of queryParameters #behaviorExpected")
    def "When no uri filters exist"(){

        repose.updateConfigs("features/filters/normalization/uri/onlymediavariant")


        given:
        def path = "/" + matchingUriRegex + "/?" + qpBeforeRepose;

        when: "A request is made to REPOSE"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + path, method)

        then: "Request is forwarded to origin service"
        mc.handlings.size() == 1
        def Handling handling = mc.handlings.get(0)

        then: "Request sent to origin service matches expected query parameter list"
        handling.request.path.endsWith(qpAfterRepose)

        where:
        method   | matchingUriRegex      | qpBeforeRepose                                               | qpAfterRepose                                               | behaviorExpected
        "GET"    | "only_media_variant"  | "filter_me=true&a=1&a=4&a=2&r=1241.212&n=test&a=Add+Space"   | "filter_me=true&a=1&a=4&a=2&a=Add+Space&r=1241.212&n=test" | "Should not filter any query parameters"

    }
}