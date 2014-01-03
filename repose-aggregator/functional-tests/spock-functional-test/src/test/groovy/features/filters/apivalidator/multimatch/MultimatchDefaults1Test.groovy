package features.filters.apivalidator.multimatch

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class MultimatchDefaults1Test extends ReposeValveTest {

    static def params

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multimatch/m-default-1", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    // This TestCase checks that the default runs after skips and failures.
    def "When multi-role-match is set and a default validator passes the request (MF5(P)F4\\3 = MPF5PF4\\1,4 -> MPNNN -> P)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings
        "role-3" | "200"        | 1
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
