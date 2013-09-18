package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Api Validator q-value tests ported from python
 */
@Category(Slow.class)
class QValueTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/f4f5p")
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
    def "When single match q-value"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                          | responseCode | numHandlings
        "role-1; q=0.1, role-3; q=0.9" | "200"        | 1
    }

    def "When multi match q-value"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/mf4p")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                          | responseCode | numHandlings
        "role-1; q=0.9, role-2; q=0.1" | "404"        | 0
    }

    def "When all roles have same high q-value"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/f4f5p")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                         | responseCode | numHandlings
        "role-3; q=0.9, role-2; q=0.1, role-1; q=0.9" | "404"        | 0
    }
}
