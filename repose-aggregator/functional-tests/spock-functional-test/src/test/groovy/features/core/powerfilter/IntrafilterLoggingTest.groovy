package features.core.powerfilter

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import groovy.json.JsonSlurper
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

    def setup(){
        reposeLogSearch.cleanLog()
    }

    def "Request entry with client-auth and ip-identity filter"() {
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/server123", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityService.client_token])

        def sentRequest = mc.getHandlings()[0]
        def user = sentRequest.request.headers.findAll("x-pp-user")[1]

        then: "checking for response code and handlings"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        reposeLogSearch.searchByString("TRACE intrafilter-logging").size() == 4
        reposeLogSearch.searchByString("Intrafilter Request Log").size() == 2
        reposeLogSearch.searchByString("Intrafilter Response Log").size() == 2

        // This part of test what will be in the TRACE log to expect
        and: "checking for client-auth - request"

        JSONObject authreqline1 = convertToJson("Intrafilter Request Log", 0)
        authreqline1.get("currentFilter") == "client-auth"
        authreqline1.get("httpMethod") == "GET"
        authreqline1.get("requestURI") == "/servers/server123"
        authreqline1.get("requestBody") == ""
        authreqline1.get("headers").has("X-Auth-Token")
        authreqline1.get("headers").has("Intrafilter-UUID")
        def requestId = authreqline1.get("headers").get("Intrafilter-UUID")

        and: "checking for client-auth - response"
        //THIS IS 2ND IN THE LIST BECAUSE IT'S A STACK
        def authrespline1 = convertToJson("Intrafilter Response Log", 1)
        authrespline1.get("currentFilter") == "client-auth"
        authrespline1.get("responseBody") == ""
        authrespline1.get("httpResponseCode") == "200"
        authrespline1.get("headers").has("Intrafilter-UUID")
        println requestId
        println authrespline1.get("headers").get("Intrafilter-UUID")
        requestId == authrespline1.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - request"
        def authreqline2 = convertToJson("Intrafilter Request Log", 1)
        authreqline2.get("currentFilter") == "ip-identity"
        authreqline2.get("httpMethod") == "GET"
        authreqline2.get("requestURI") == "/servers/server123"
        authreqline2.get("requestBody") == ""
        authreqline2.get("headers").has("X-Auth-Token")
        authreqline2.get("headers").has("x-pp-user")
        authreqline2.get("headers").has("x-pp-groups")
        authreqline2.get("headers").has("x-tenant-name")
        authreqline2.get("headers").has("x-user-id")
        authreqline2.get("headers").has("x-authorization")
        authreqline2.get("headers").has("Intrafilter-UUID")
        println authreqline2.get("headers").get("Intrafilter-UUID")
        requestId == authreqline2.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - response"
        //THIS IS FIRST ON THE LIST BECAUSE IT'S A STACK
        def authrespline2 = convertToJson("Intrafilter Response Log", 0)
        authrespline2.get("currentFilter") == "ip-identity"
        authrespline2.get("responseBody") == ""
        authrespline2.get("httpResponseCode") == "200"
        authrespline2.get("headers").has("Intrafilter-UUID")
        println authrespline2.get("headers").get("Intrafilter-UUID")
        requestId == authrespline2.get("headers").get("Intrafilter-UUID")
    }

    private JSONObject convertToJson(String searchString, int entryNumber ){
        def reqString = reposeLogSearch.searchByString(searchString).get(entryNumber)
        def requestJson = reqString.substring(reqString.indexOf("{\"preamble\""))
        println requestJson

        return new JSONObject(requestJson)
    }
}
