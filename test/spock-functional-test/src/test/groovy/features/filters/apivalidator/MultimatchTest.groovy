package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Api Validator multimatch tests ported from python
 */
@Category(Slow.class)
class MultimatchTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/f4f4pf5f5")
        repose.start()

        sleep(5000)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    // This test must run first (due to config loading)
    def "When a request is made with role(s) matching a validator"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-3"        | "200"        | 1
        "role-3,role-4" | "200"        | 1
        "role-4,role-3" | "200"        | 1
        "role-2,role-3" | "404"        | 0
        "role-3,role-2" | "404"        | 0
    }

    def "When a request is made with a role not matching a validator and no default validator"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/p")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-0"        | "403"        | 0
        "role-1"        | "200"        | 1
    }

    def "When a request is made to a resource that is not defined in the wadl"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/f4")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-1"        | "404"        | 0
    }

    def "When a request is made to a resource that is not defined in the wadl with multiple validators"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/pf4f5")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-2"        | "404"        | 0
    }

    def "When a request is made and a default validator is set"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/s-default")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-1"        | "404"        | 0
        "role-0"        | "405"        | 0
    }

    def "When multi-role-match is set"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/mf4f4f5f4f5f5pf4f4")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings
        "role-3,role-5,role-6,role-7"        | "200"        | 1
        "role-3,role-5,role-6"               | "405"        | 0
        "role-7,role-8"                      | "200"        | 1
        "role-7,role-3"                      | "200"        | 1
    }

    def "When multi-role-match is set and no validator matches"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/mp")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-0" | "403"        | 0
        "role-1" | "200"        | 1
    }

    def "When multi-role-match is set and a fail validator matches the role"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/mf4")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-1" | "404"        | 0
    }

    def "When multi-role-match is set and a pass validator matches the role"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/mf4p")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-2" | "200"        | 1
    }

    def "When multi-role-match is set and a default validator passes the request"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/m-default-1")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-3" | "200"        | 1
    }

    def "When multi-role-match is set and a matching validator passes the request"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/m-default-2")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-3" | "200"        | 1
    }

    def "When multi-role-match is set and a matching validator fails the request"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/m-default-3")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-3" | "405"        | 0
    }

    def "When multi-role-match is set and a default validator fails the request"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/m-default-4")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-0" | "404"        | 0
    }
}
