package com.rackspace.lefty.tenant

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Created by adrian on 6/12/17.
 */
class TenantCullingFilterFunctionalTest extends ReposeValveTest {
    def setupSpec() {
        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def "Just passes though"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "200"
    }
}
