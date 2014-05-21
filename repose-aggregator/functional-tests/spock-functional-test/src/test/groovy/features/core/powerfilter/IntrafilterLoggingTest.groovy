package features.core.powerfilter
import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
/**
 * Created by jennyvo on 5/19/14.
 * This test checking TRACE log configuration.
 */
class IntrafilterLoggingTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        //remove old log
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.deleteLog()

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/intrafilterlogging", params)

        repose.start([waitOnJmxAfterStarting:false])

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "Request entry with client-auth and ip-identity filter"() {
        def logSearch = new ReposeLogSearch(properties.logFile)
        logSearch.cleanLog()

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/server123", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityService.client_token])

        def sentRequest = mc.getHandlings()[0]
        def user = sentRequest.request.headers.findAll("x-pp-user")[1]

        then: "checking for response code and handlings"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        // This part of test by imagination of what will be in the TRACE log to expect
        and: "checking for client-auth - enter"
        logSearch.searchByString("client-auth - enter: Headers: .* Content: .* Status Code: .* Request Path: .*").size() == 1

        and: "checking for client-auth - exit"
        def client_auth_exit = logSearch.searchByString("client-auth - exit: Headers: .* Content: .* Status Code: .* Request Path: .*")
        client_auth_exit.size() == 1
        def exit_content = client_auth_exit.toString().substring(22)

        and: "checking for ip-identity - enter"
        def ip_identity_enter = logSearch.searchByString("client-auth - enter: Headers: .* Content: .* Status Code: .* Request Path: .*")
        ip_identity_enter.size() == 1
        def enter_content = ip_identity_enter.toString().substring(22)
        exit_content == enter_content

        and: "checking for ip-identity - exit"
        logSearch.searchByString("client-auth - exit: Headers: .* Content: .* Status Code: .* Request Path: .*").size() == 1

        and: "additional checking for ip"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 2
        user == "127.0.0.1;q=0.4" || user == "0:0:0:0:0:0:0:1;q=0.4" | user == "::1;q=0.4"
        sentRequest.request.headers.findAll("x-pp-groups")[1].equalsIgnoreCase("IP_Standard;q=0.4")
    }
}
