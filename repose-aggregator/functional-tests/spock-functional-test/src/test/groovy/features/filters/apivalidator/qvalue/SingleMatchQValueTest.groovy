package features.filters.apivalidator.qvalue

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class SingleMatchQValueTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/qvalue/f4f5p", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "Should use the roles with the highest qvalue (f4f5p\\1q0.1,3q0.9 -> p)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                         | responseCode | numHandlings | Description
        "role-1; q=0.1, role-3; q=0.9"                | "200"        | 1            | "test_single_match_qvalue"
    }

    def "When multiple roles have the same qvalue, and that qvalue is the highest, use all of them (f4f5p\\3q0.9,2q0.1,1q0.9 -> f4)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                         | responseCode | numHandlings | Description
        "role-3; q=0.9, role-2; q=0.1, role-1; q=0.9" | "404"        | 0            | "test_use_all_roles_with_the_same_high_qvalue"
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
