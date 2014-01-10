package features.filters.apivalidator.multipleRoles

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class RoleOrderTest extends ReposeValveTest{

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/multipleRoles/p{2}f4{1,2}", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

    }

    // The order of the validators in the config file should be respected, and
    // not the order of roles specified in the request
    def "When roles for both validators are given in the request, the first validator should match (p{2}f4{1,2}\\1,2 -> p)"() {
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
