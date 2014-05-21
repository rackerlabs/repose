package features.core.powerfilter
import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.codehaus.jettison.json.JSONObject
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
        logSearch.searchByString("TRACE intrafilter-logging").size() == 4
        logSearch.searchByString("Intrafilter Request Log").size() == 2
        logSearch.searchByString("intrafilter Response Log").size() == 2

        // This part of test what will be in the TRACE log to expect
        and: "checking for client-auth - enter"
        def firstReq = logSearch.searchByString("Intrafilter Request Log").get(0)
        JSONObject authreqline = new JSONObject(firstReq)
        authreqline.getString("currentFilter") == "client-auth"
        authreqline.getString("httpMethod") == "GET"

        and: "checking for client-auth - exit"
        def firstResp = logSearch.searchByString("Intrafilter Response Log").get(0)
        JSONObject authrespline = new JSONObject(firstResp)
        authrespline.getString("currentFilter") == "client-auth"
        authrespline.getString("httpMethod") == "GET"
        def getrespheader = authrespline.getString("headers")

        and: "checking for ip-identity - enter"
        def secondReq = logSearch.searchByString("Intrafilter Request Log").get(1)
        JSONObject ipidentityreqline = new JSONObject(secondReq)
        ipidentityreqline.getString("currentFilter") == "ip-identity"
        ipidentityreqline.getString("httpMethod") == "GET"
        ipidentityreqline.getString("headers") == getrespheader

        and: "checking for ip-identity - exit"
        def secondResp = logSearch.searchByString("Intrafilter Response Log").get(1)
        JSONObject ipidentityrespline = new JSONObject(secondResp)
        ipidentityrespline.getString("currentFilter") == "ip-identity"
        ipidentityrespline.getString("httpMethod") == "GET"

        and: "additional checking for ip"
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 2
        user == "127.0.0.1;q=0.4" || user == "0:0:0:0:0:0:0:1;q=0.4" | user == "::1;q=0.4"
        sentRequest.request.headers.findAll("x-pp-groups")[1].equalsIgnoreCase("IP_Standard;q=0.4")
    }
}
