package features.core.intrafilterlogging

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * Created by jennyvo on 7/16/15.
 * Verify Repose no longer logging 'null' as part of currunt Filter description
 */
class IntraFilterLoggingTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/intrafilterlogging", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Verify intra filter log for current filter no longer have 'null' in the description" () {
        given:
        // repose start up

        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        reposeLogSearch.searchByString("\"currentFilter\":\"rackspace-identity-basic-auth\"").size() > 0
        reposeLogSearch.searchByString("\"currentFilter\":\"herp\"").size() > 0
        reposeLogSearch.searchByString("null-rackspace-identity-basic-auth").size() == 0
        reposeLogSearch.searchByString("null-herp").size() == 0
    }
}
