package features.filters.apivalidator.multimatch

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class PatternSfnTest extends ReposeValveTest {

    static def params

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multimatch/pf4f5", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "When a request is made to a resource that is not defined in the wadl with multiple validators (PF4F5\\2 -> SF4N -> F4)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-2"        | "404"        | 0
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
