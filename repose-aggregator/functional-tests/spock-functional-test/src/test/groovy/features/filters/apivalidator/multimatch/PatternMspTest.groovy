package features.filters.apivalidator.multimatch

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class PatternMspTest extends ReposeValveTest {

    static def params

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multimatch/mf4p", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "When multi-role-match is set and a pass validator matches the role (MF4P\\2 -> MSP -> P)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-2" | "200"        | 1
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
