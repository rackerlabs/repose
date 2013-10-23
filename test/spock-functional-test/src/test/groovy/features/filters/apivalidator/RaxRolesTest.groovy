package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

/**
 * A test to verify that a user can validate roles via api-checker
 * by setting the enable-rax-roles attribute on a validator.
 */
class RaxRolesTest extends ReposeValveTest{

    def okHandler = {return new Response(200)}

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/raxroles")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if(repose)
            repose.stop()
        if(deproxy)
            deproxy.shutdown()
    }

    @Unroll("User1:method="+method+",headers="+headers.toString()+",expected response="+responseCode)
    def "when enable-rax-role is false, user validates the entire wadl"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers,
                defaultHandler: okHandler)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                            | responseCode
        "GET"    | ["x-roles": "test_user1"]          | "200"
        "PUT"    | ["x-roles": "test_user1"]          | "200"
        "POST"   | ["x-roles": "test_user1"]          | "200"
        "DELETE" | ["x-roles": "test_user1"]          | "200"
    }

    @Unroll("User2:method="+method+",headers="+headers.toString()+",expected response="+responseCode)
    def "when enable-rax-roles is true, validate with wadl method level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers,
                defaultHandler: okHandler)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                            | responseCode
        "GET"    | ["x-roles": "test_user2"]          | "403"
        "GET"    | ["x-roles": "a:observer"]          | "200"
        "GET"    | ["x-roles": "a:observer, a:bar"]   | "200"
        "GET"    | ["x-roles": "a:observer, a:bar"]   | "403"
        "GET"    | ["x-roles": "a:bar, a:admin"]      | "403"
        "GET"    | ["x-roles": "a:admin"]             | "403"
        "GET"    | null                               | "403"
        "PUT"    | ["x-roles": "test_user2"]          | "200"
        "PUT"    | ["x-roles": "a:bar"]               | "200"
        "PUT"    | ["x-roles": "a:observer, a:bar"]   | "200"
        "PUT"    | ["x-roles": "a:bar, a:jawsome"]    | "200"
        "PUT"    | ["x-roles": "a:admin"]             | "200"
        "PUT"    | null                               | "200"
        "POST"   | ["x-roles": "test_user2"]          | "403"
        "POST"   | ["x-roles": "a:observer"]          | "403"
        "POST"   | ["x-roles": "a:admin"]             | "200"
        "POST"   | ["x-roles": "a:bar, a:admin"]      | "200"
        "POST"   | ["x-roles": "a:bar"]               | "403"
        "POST"   | ["x-roles": "a:bar, a:observer"]   | "403"
        "POST"   | null                               | "403"
        "DELETE" | ["x-roles": "test_user2"]          | "403"
        "DELETE" | ["x-roles": "a:observer, a:bar"]   | "200"
        "DELETE" | ["x-roles": "a:admin, a:bar"]      | "200"
        "DELETE" | ["x-roles": "a:bar, a:admin"]      | "200"
        "DELETE" | ["x-roles": "a:observer, a:admin"] | "200"
        "DELETE" | ["x-roles": "a:observer"]          | "200"
        "DELETE" | ["x-roles": "a:admin"]             | "200"
        "DELETE" | ["x-roles": "a:bar"]               | "403"
        "DELETE" | ["x-roles": "a:bar, a:jawsome"]    | "403"
        "DELETE" | ["x-roles": "observer, creator"]   | "403"
        "DELETE" | null                               | "403"
    }

    @Unroll("User3:method="+method+",headers="+headers.toString()+",expected response="+responseCode)
    def "when enable-rax-roles is true, validate with wadl resource level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers,
                defaultHandler: okHandler)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                            | responseCode

    }
}
