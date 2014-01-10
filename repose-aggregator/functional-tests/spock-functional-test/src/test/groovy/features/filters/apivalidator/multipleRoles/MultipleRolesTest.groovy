package features.filters.apivalidator.multipleRoles

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class MultipleRolesTest extends ReposeValveTest{

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multipleRoles/p{1,2}", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")
    }

    def "When no role matches, it should return 403 (p{1,2}\\0 -> f3)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-0"        | "403"        | 0            | "test_neither_role"
    }

    def "When the first role is given in the request, it should match (p{1,2}\\1 -> p)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-1"        | "200"        | 1            | "test_first_role"
    }

    def "When the second role is given in the request, it should match (p{1,2}\\2 -> p)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-2"        | "200"        | 1            | "test_second_role"
    }

    def "When both roles are given in the request, it should match (p{1,2}\\1,2 -> p)"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-1,role-2" | "200"        | 1
    }

    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
