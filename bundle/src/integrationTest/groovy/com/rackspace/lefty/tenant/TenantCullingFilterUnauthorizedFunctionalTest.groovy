package com.rackspace.lefty.tenant

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED

class TenantCullingFilterUnauthorizedFunctionalTest extends ReposeValveTest {
    static final String AUTH_TOKEN_KEY = "X-Auth-Token-Key"

    def setupSpec() {
        deproxy = new Deproxy()
        def params = properties.defaultTemplateParams
        deproxy.addEndpoint(properties.targetPort)
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('noKeystone', params)
        repose.start()
    }

    @Unroll
    def "#testName returns Unauthorized"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then:
        mc.handlings.size() == 0
        mc.receivedResponse.code as Integer == SC_UNAUTHORIZED

        where:
        testName       | headers
        'Cache miss'   | [(AUTH_TOKEN_KEY): 'Value']
        'No cache key' | null
    }
}
