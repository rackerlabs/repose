package features.filters.apivalidator.multimatch

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class PatternSspnnTest extends ReposeValveTest {

    static def params

    def setupSpec() {

        params = properties.getDefaultTemplateParams()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multimatch/f4f4pf5f5", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "A pass should stop processing (F4F4PF5F5\\3 -> SSPNN -> P)"() {
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
    }

    def "With multiple roles given, the first one encountered in the config should be used (F4F4PF5F5\\3,4 -> SSPNN -> P)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-3,role-4" | "200"        | 1
    }

    def "With multiple roles given, the order should not matter (F4F4PF5F5\\4,3 -> SSPNN -> P)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-4,role-3" | "200"        | 1
    }

    def "With multiple roles given, the first one encountered in the config should be used (F4F4PF5F5\\2,3 -> SF4NNN -> F4)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-2,role-3" | "404"        | 0
    }

    def "With multiple roles given, the order should not matter (F4F4PF5F5\\3,2 -> SF4NNN -> F4)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-3,role-2" | "404"        | 0
    }

    def cleanupSpec() {

        if (repose)
            repose.stop()

        if (deproxy)
            deproxy.shutdown()
    }
}
