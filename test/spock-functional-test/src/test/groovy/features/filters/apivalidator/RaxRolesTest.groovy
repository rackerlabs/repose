package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

/**
 * A test to verify that a user can validate roles via api-checker
 * by setting the enable-rax-roles attribute on a validator.
 */
class RaxRolesTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/raxroles")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("raxRolesDisabled:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-role is false, user authorized to access the entire wadl"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                         | responseCode
        "GET"    | ["x-roles": "raxRolesDisabled"] | "200"
        "PUT"    | ["x-roles": "raxRolesDisabled"] | "200"
        "POST"   | ["x-roles": "raxRolesDisabled"] | "200"
        "DELETE" | ["x-roles": "raxRolesDisabled"] | "200"
    }

    @Unroll("raxRolesEnabled:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl method level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                                             | responseCode
        "GET"    | ["x-roles": "raxRolesEnabled"]                      | "403"
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "403"
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "403"
        "GET"    | null                                                | "403"
        "PUT"    | ["x-roles": "raxRolesEnabled"]                      | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar"]               | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "POST"   | ["x-roles": "raxRolesEnabled"]                      | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:observer"]          | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar"]               | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:observer"]   | "403"
        "POST"   | null                                                | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled"]                      | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin, a:bar"]      | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:admin"] | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar"]               | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, observer, creator"]   | "403"
        "DELETE" | null                                                | "403"
    }

//    @Unroll("User3:method=#method,headers=#headers,expected response=#responseCode")
//    def "when enable-rax-roles is true, validate with wadl resource level roles"() {
//        given:
//        MessageChain messageChain
//
//        when:
//        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)
//
//        then:
//        messageChain.getReceivedResponse().getCode().equals(responseCode)
//    }
}
