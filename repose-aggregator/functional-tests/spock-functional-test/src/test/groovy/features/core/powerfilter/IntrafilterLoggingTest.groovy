package features.core.powerfilter

import framework.ReposeLogSearch
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.codehaus.jettison.json.JSONObject
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

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

    @Unroll("when sending token #sendtoken respcode will be #respcode")
    def "Check TRACE log entries for when req sent to Repose with client-auth and ip-identity filter"() {
        given:
        fakeIdentityService.client_token = sendtoken

        def responseBody = respbody
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/server123", method: 'GET',
                headers: ['X-Auth-Token': fakeIdentityService.client_token],
                defaultHandler: { request ->
                    return new Response(respcode, respmsg, ["Content-Type": type], responseBody) })

        //def sentRequest = mc.getHandlings()[0]

        then: "checking for response code and handlings"
        mc.receivedResponse.code == respcode
        mc.handlings.size() == 1
        reposeLogSearch.searchByString("TRACE intrafilter-logging").size() == 4
        reposeLogSearch.searchByString("Intrafilter Request Log").size() == 2
        reposeLogSearch.searchByString("Intrafilter Response Log").size() == 2

        // This part of test what will be in the TRACE log to expect
        and: "checking for client-auth - request"

        JSONObject authreqline1 = convertToJson("Intrafilter Request Log", 0)
        assertHeadersExists(["X-Auth-Token","Intrafilter-UUID"], authreqline1)
        assertKeyValueMatch([
                "currentFilter": "client-auth",
                "httpMethod": "GET",
                "requestURI": "/servers/server123",
                "requestBody": ""
        ], authreqline1)
        def requestId = authreqline1.get("headers").get("Intrafilter-UUID")

        and: "checking for client-auth - response"
        //THIS IS 2ND IN THE LIST BECAUSE IT'S A STACK
        def authrespline1 = convertToJson("Intrafilter Response Log", 1)
        assertHeadersExists(["Intrafilter-UUID","Content-Type"], authrespline1)
        assertKeyValueMatch([
                "currentFilter": "client-auth",
                "responseBody": responseBody,
                "httpResponseCode": respcode
        ], authrespline1)
        requestId == authrespline1.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - request"
        def authreqline2 = convertToJson("Intrafilter Request Log", 1)
        assertHeadersExists(["X-Auth-Token","x-pp-user", "x-pp-groups","x-tenant-name",
                             "x-user-id", "x-authorization", "Intrafilter-UUID"], authreqline2)
        assertKeyValueMatch([
                "currentFilter": "ip-identity",
                "httpMethod": "GET",
                "requestURI": "/servers/server123",
                "requestBody": ""
        ], authreqline2)
        requestId == authreqline2.get("headers").get("Intrafilter-UUID")

        and: "checking for ip-identity - response"
        //THIS IS FIRST ON THE LIST BECAUSE IT'S A STACK
        def authrespline2 = convertToJson("Intrafilter Response Log", 0)
        assertHeadersExists(["Intrafilter-UUID","Content-Type"], authrespline2)
        assertKeyValueMatch([
                "currentFilter": "ip-identity",
                "responseBody": responseBody,
                "httpResponseCode": respcode
        ], authrespline2)
        requestId == authrespline2.get("headers").get("Intrafilter-UUID")

        where:
        sendtoken                   |respcode   |respmsg            |type               |respbody
        "this-is-a-token"           | "200"     | "OK"              |"application/json" |"{\"response\": \"amazing\""
        "this-is-an-invalid-token"  | "401"     | "Invalid Token"   |"plain/text"       |"{\"response\": \"Unauthorized\""
    }

    private JSONObject convertToJson(String searchString, int entryNumber ){
        def reqString = reposeLogSearch.searchByString(searchString).get(entryNumber)
        def requestJson = reqString.substring(reqString.indexOf("{\"preamble\""))
        println requestJson

        return new JSONObject(requestJson)
    }

    private assertHeadersExists(List headers, JSONObject jsonObject){
        headers.each {
            headerName ->
                assert jsonObject.get("headers").has(headerName)
        }
    }

    private assertKeyValueMatch(Map headers, JSONObject jsonObject){
        headers.each {
            key, value ->
                assert jsonObject.get(key) == value

        }

    }
}
