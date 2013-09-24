package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Api Validator q-value tests ported from python

    qvalue tests
    ------------

    Description:
    "Take into account qvalue. So don't match against all X-Roles values but
    only those with highest qvalue. If the highest qvalue is 0.9, and two
    values have that qvalue, use those two to compare against @role, and no
    others."

    Notation:
    When a role designation is followed by a 'q' and a number, that indicates
    the qvalue that the associated header value will have when sent to Repose.
    For example, '1q0.5' translates into 'X-Roles: role-1; q=0.5'. If no 'q'
    and no number are given, then no 'q=' will be added to the header, and the
    qvalue will assume the HTTP default of 1


    We want to ensure that the filter is using the role(s) with highest qvalue, and
    discarding all others. To do this, we define a few validator configs and send
    requests with roles of various qvalues:

    f4f5p\1q0.1,3q0.9 -> p
    mf4p\1q0.9,2q0.1 -> f4

    If qvalues were ignored, then the first test would result in an f4, and the
    second would result in a pass.

    We also want to check that the filter is picking all roles that share the
    highest qvalue, instead of just the first one it finds.

    f4f5p\3q0.9,2q0.1,1q0.9 -> f4

    Both role-1 and role-3 will have q=0.9, and role-2 should be discarded. If the
    filter just uses the first role-3 value, then it will result in a pass. If it
    uses both role-1 and role-3, then it will result in an f4.

 *
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

    // If more than one role has the highest qvalue, use all of them.
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
