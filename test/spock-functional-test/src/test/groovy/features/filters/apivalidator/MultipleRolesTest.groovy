package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Api Validator multiple roles tests ported from python
 */
@Category(Slow.class)
class MultipleRolesTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/p{1,2}")
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
    def "When multiple roles are used"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-0"        | "403"        | 0
        "role-1"        | "200"        | 1
        "role-2"        | "200"        | 1
        "role-1,role-2" | "200"        | 1
    }

    def "When roles are ordered"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/p{2}f4{1,2}")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-1,role-2" | "200"        | 1
    }
}
