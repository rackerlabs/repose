package features.filters.apivalidator.multimatch

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class PatternMssfsffpnnTest extends ReposeValveTest {

    static def params

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multimatch/mf4f4f5f4f5f5pf4f4", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "Failures should not stop processing (MF4F4F5F4F5F5PF4F4\\3,5,6,7 -> MSSF5SF5F5PNN -> P)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings
        "role-3,role-5,role-6,role-7"        | "200"        | 1
    }

    def "If none pass, the last failure should be used (MF4F4F5F4F5F5PF4F4\\3,5,6 -> MSSF5SF5F5SSS -> F5)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings
        "role-3,role-5,role-6"               | "405"        | 0
    }

    def "A pass without failures should stop processing (MF4F4F5F4F5F5PF4F4\\7,8 -> MSSSSSSPNN -> F5)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings
        "role-7,role-8"                      | "200"        | 1
    }

    def "The order of the values in X-Roles should not affect the order of processing the validators (MF4F4F5F4F5F5PF4F4\\7,3 -> MSSF5SSSPNN -> F5)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings
        "role-7,role-3"                      | "200"        | 1
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
