package features.filters.identitybasicauth
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders
/**
 * Created by jennyvo on 11/12/14.
 * Delegating with identity basic auth test
 */
class BasicAuthDelegatingTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
        fakeIdentityService.checkTokenValid = true
    }

    def setup() {
        fakeIdentityService.with {
            // This is required to ensure that one piece of the authentication data is changed
            // so that the cached version in the Akka Client is not used.
            client_apikey = UUID.randomUUID().toString()
            client_token = UUID.randomUUID().toString()
        }
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "Retrieve a token for an HTTP Basic authentication header with UserName/ApiKey"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get a token for it"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
    }

    @Unroll ("#method with #caseDesc")
    def "No HTTP Basic authentication header sent and no token with delegating."() {
        given:
        def headers = ""
        if(caseDesc == "Invalid key or username") {
            headers = [
                    (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":BAD-API-KEY").bytes)
            ]
        } else {
            headers = ""
        }

        when: "the request does not have an HTTP Basic authentication or invalid key/username"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: headers)

        then: "simply pass it on down the filter chain and this configuration will forward to origin service a SC_UNAUTHORIZED (401)"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated").contains(delegatedMsg)
        //mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        //mc.orphanedHandlings.size() == 0

        where:
        caseDesc                        | method      | delegatedMsg
        "No HTTP Basic authentication"  | "GET"       | "401 Unauthorized"
        "No HTTP Basic authentication"  | "PUT"       | "401 Unauthorized"
        "No HTTP Basic authentication"  | "POST"      | "401 Unauthorized"
        "No HTTP Basic authentication"  | "DELETE"    | "401 Unauthorized"
        "No HTTP Basic authentication"  | "PATCH"     | "401 Unauthorized"
        "Invalid key or username"       | "GET"       | "401 Unauthorized"
        "Invalid key or username"       | "PUT"       | "401 Unauthorized"
        "Invalid key or username"       | "POST"      | "401 Unauthorized"
        "Invalid key or username"       | "DELETE"    | "401 Unauthorized"
        "Invalid key or username"       | "PATCH"     | "401 Unauthorized"
    }

    @Unroll("Sending request with auth admin response set to HTTP #identityStatusCode")
    def "when failing to authenticate admin client"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key and the Mock Identity Service's generateTokenHandler"
        fakeIdentityService.with {
            generateTokenHandler = {
                request, xml ->
                    new Response(identityStatusCode, null, null, null)
            }
        }
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "user passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated").contains(delegatedMsg)

        where:
        reqTenant | identityStatusCode                           | delegatedMsg //(these msgs need to be update when done with impl
        9400      | HttpServletResponse.SC_BAD_REQUEST           | "500 Server Error"
        9401      | HttpServletResponse.SC_UNAUTHORIZED          | "401 Unauthorized"
        9403      | HttpServletResponse.SC_FORBIDDEN             | "500 Server Error"
        9404      | HttpServletResponse.SC_NOT_FOUND             | "500 Server Error"
        9500      | HttpServletResponse.SC_INTERNAL_SERVER_ERROR | "500 Server Error"
        9501      | HttpServletResponse.SC_NOT_IMPLEMENTED       | "500 Server Error"
        9502      | HttpServletResponse.SC_BAD_GATEWAY           | "500 Server Error"
        9503      | HttpServletResponse.SC_SERVICE_UNAVAILABLE   | "500 Server Error"
        9504      | HttpServletResponse.SC_GATEWAY_TIMEOUT       | "500 Server Error"
    }
}
