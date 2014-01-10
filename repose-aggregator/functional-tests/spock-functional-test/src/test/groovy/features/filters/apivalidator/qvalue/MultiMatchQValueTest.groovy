package features.filters.apivalidator.qvalue

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class MultiMatchQValueTest extends ReposeValveTest {

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/qvalue/mf4p", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "Should use the role with highest qvalue (mf4p\\1q0.9,2q0.1 -> f4)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                          | responseCode | numHandlings | Description
        "role-1; q=0.9, role-2; q=0.1" | "404"        | 0            | "test_multi_match_qvalue"
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
