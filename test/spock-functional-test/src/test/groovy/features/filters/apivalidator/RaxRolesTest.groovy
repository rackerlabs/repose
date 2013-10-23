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

    @Unroll("User3:method=#method,headers=#headers,expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl resource level roles"() {
        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method,
                headers: headers)
        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | headers                                        | responseCode
        "GET"    | ["x-roles": "test_user3, a:admin"]             | "200"
        "GET"    | ["x-roles": "test_user3, a:observer"]          | "403"
        "GET"    | ["x-roles": "test_user3, b:observer"]          | "403"
        "GET"    | ["x-roles": "test_user3, b:creator"]           | "403"
        "PUT"    | ["x-roles": "test_user3, a:admin"]             | "200"
        "PUT"    | ["x-roles": "test_user3, a:observer"]          | "200"
        "PUT"    | ["x-roles": "test_user3, a:observer, a:admin"] | "200"
        "PUT"    | ["x-roles": "test_user3, a:bar"]               | "403"
        "PUT"    | ["x-roles": "test_user3"]                      | "403"
        "PUT"    | ["x-roles": "test_user3, a:observe"]           | "403"
        "POST"   | ["x-roles": "test_user3, a:observer"]          | "403"
        "POST"   | ["x-roles": "test_user3, a:admin"]             | "200"
        "DELETE" | ["x-roles": "test_user3, a:admin"]             | "200"
        "DELETE" | ["x-roles": "test_user3, a:observer"]          | "200"
        "DELETE" | ["x-roles": "test_user3, a:observer, a:admin"] | "200"
        "DELETE" | ["x-roles": "test_user3, a:creator"]           | "200"
        "DELETE" | ["x-roles": "test_user3, a:bar"]               | "403"
        "DELETE" | ["x-roles": "test_user3"]                      | "403"
        "DELETE" | ["x-roles": "test_user3, a:observe"]           | "403"
        "GET"    | null                                           | "403"
        "PUT"    | null                                           | "403"
        "POST"   | null                                           | "403"
        "DELETE" | null                                           | "403"
    }

    @Unroll("User5:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true and wadl has roles with #all"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method | path   | headers                                      | responseCode
        "GET"  | "/a"   | ["x-roles": "test_user5, a:observer"]        | "200"
        "GET"  | "/a"   | ["x-roles": "test_user5, a:observer, a:bar"] | "200"
        "GET"  | "/a"   | ["x-roles": "test_user5, a:bar"]             | "200"
        "GET"  | "/a"   | ["x-roles": "test_user5, a:abar, a:admin"]   | "200"
        "GET"  | "/a"   | ["x-roles": "test_user5, a:admin"]           | "200"
        "GET"  | "/a"   | ["x-roles": "test_user5"]                    | "200"
        "PUT"  | "/a"   | ["x-roles": "test_user5, a:admin"]           | "200"
        "PUT"  | "/a"   | ["x-roles": "test_user5, a:bar, a:admin"]    | "200"
        "PUT"  | "/a"   | ["x-roles": "test_user5, a:observer, a:bar"] | "403"
        "PUT"  | "/a"   | ["x-roles": "test_user5"]                    | "403"
        "GET"  | "/b"   | ["x-roles": "test_user5, a:admin"]           | "200"
        "GET"  | "/b"   | ["x-roles": "test_user5"]                    | "200"
        "GET"  | "/b"   | ["x-roles": "test_user5, bar"]               | "200"
        "POST" | "/b"   | ["x-roles": "test_user5, a:admin"]           | "405"
        "POST" | "/b"   | ["x-roles": "test_user5"]                    | "405"
        "POST" | "/b/c" | ["x-roles": "test_user5, a:admin"]           | "200"
        "POST" | "/b/c" | ["x-roles": "test_user5"]                    | "200"
        "POST" | "/b/c" | ["x-roles": "test_user5, bar"]               | "200"

    }

    @Unroll("User7:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is false and check-headers does not affect it"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path | headers                                | responseCode
        "PUT"    | "/a" | ["x-roles": "test_user7, a:noone"]     | "200"
        "PUT"    | "/a" | ["x-roles": "test_user7, a:creator"]   | "200"
        "PUT"    | "/a" | ["x-roles": "test_user7"]              | "200"
        "DELETE" | "/a" | ["x-roles": "test_user7"]              | "405"
        "GET"    | "/b" | ["x-roles": "test_user7, a:noone"]     | "404"

        "PUT"    | "/a" | ["x-roles": "test_user7.1, a:noone"]   | "200"
        "PUT"    | "/a" | ["x-roles": "test_user7.1, a:creator"] | "200"
        "PUT"    | "/a" | ["x-roles": "test_user7.1"]            | "200"
        "DELETE" | "/a" | ["x-roles": "test_user7.1"]            | "405"
        "GET"    | "/b" | ["x-roles": "test_user7.1, a:noone"]   | "404"

    }

    @Unroll("User8:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Rax Roles will not inherit from siblings"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method | path   | headers                                      | responseCode
        "PUT"  | "/a"   | ["x-roles": "test_user8, a:observer, a:bar"] | "403"
        "PUT"  | "/a"   | ["x-roles": "test_user8"]                    | "403"
        "GET"  | "/a"   | ["x-roles": "test_user8, a:admin"]           | "200"
        "GET"  | "/b"   | ["x-roles": "test_user8, a:noone"]           | "200"
        "GET"  | "/b"   | ["x-roles": "test_user8, a:creator"]         | "200"
        "GET"  | "/b/c" | ["x-roles": "test_user8"]                    | "200"
        "POST" | "/b/c" | ["x-roles": "test_user8"]                    | "200"
    }

    @Unroll("User9:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Wadl has nested resources"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                        | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user9, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user9"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user9, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user9"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user9, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user9, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user9, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user9"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user9, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user9"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user9, observer"]            | "403"
    }

    @Unroll("User10:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Remove Duplications is true"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                        | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user10, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user10"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user10, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user10"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user10, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user10, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user10, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user10"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user10, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user10"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user10, observer"]            | "403"
    }

    @Unroll("User11:method=#method,headers=#headers,expected response=#responseCode path=#path")
    def "when enable-rax-roles is true, Check Headers is false"() {

        given:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method   | path   | headers                                        | responseCode
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:admin"]             | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:observer"]          | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, a:admin, a:observer"] | "200"
        "PUT"    | "/a"   | ["x-roles": "test_user11, b:observer"]          | "403"
        "PUT"    | "/a"   | ["x-roles": "test_user11"]                      | "403"

        "DELETE" | "/a"   | ["x-roles": "test_user11, a:admin"]             | "405"
        "DELETE" | "/a"   | ["x-roles": "test_user11"]                      | "405"

        "POST"   | "/a/b" | ["x-roles": "test_user11, a:admin"]             | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user11, b:creator"]           | "200"
        "POST"   | "/a/b" | ["x-roles": "test_user11, a:observer"]          | "403"
        "POST"   | "/a/b" | ["x-roles": "test_user11"]                      | "403"

        "PUT"    | "/a/b" | ["x-roles": "test_user11, a:admin"]             | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, b:creator"]           | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, b:observer, a:foo"]   | "200"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, a:creator"]           | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user11"]                      | "403"
        "PUT"    | "/a/b" | ["x-roles": "test_user11, observer"]            | "403"
    }
}
