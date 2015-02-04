package features.filters.identitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders

import static javax.servlet.http.HttpServletResponse.*
import static org.openrepose.core.filter.logic.FilterDirector.SC_TOO_MANY_REQUESTS

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
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth/delegating", params);

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
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
    }

    @Unroll ("#method with #caseDesc")
    def "No HTTP Basic authentication header sent and no token with delegating."() {
        when: "the request does not have an HTTP Basic authentication"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method)

        then: "simply pass it on down the filter chain and this configuration will forward to origin service a SC_UNAUTHORIZED (401)"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.contains("x-delegated")

        where:
        caseDesc                        | method
        "No HTTP Basic authentication"  | "GET"
        "No HTTP Basic authentication"  | "PUT"
        "No HTTP Basic authentication"  | "POST"
        "No HTTP Basic authentication"  | "DELETE"
        "No HTTP Basic authentication"  | "PATCH"
    }

    @Unroll ("#method with #caseDesc")
    def "HTTP Basic authentication header sent and no token with delegating."() {
        given:
        def headers = [
                    (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":BAD-API-KEY").bytes)
            ]

        when: "the request has invalid key/username"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: headers)

        then: "simply pass it on down the filter chain and this configuration will forward to origin service a SC_UNAUTHORIZED (401)"
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains(delegatedMsg)
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains("q=0.2")

        where:
        caseDesc                        | method      | delegatedMsg
        "Invalid key or username"       | "GET"       | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
        "Invalid key or username"       | "PUT"       | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
        "Invalid key or username"       | "POST"      | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
        "Invalid key or username"       | "DELETE"    | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
        "Invalid key or username"       | "PATCH"     | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
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
        mc.receivedResponse.code == SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains(delegatedMsg)
        mc.handlings[0].request.headers.findAll("x-delegated")[0].contains("q=0.2")

        where:
        reqTenant | identityStatusCode          | delegatedMsg //(these msgs need to be update when done with impl
        9400      | SC_BAD_REQUEST              | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9401      | SC_UNAUTHORIZED             | "status_code=401`component=Rackspace Identity Basic Auth`message=Failed to authenticate user: $fakeIdentityService.client_username"
        9403      | SC_FORBIDDEN                | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9404      | SC_NOT_FOUND                | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9500      | SC_INTERNAL_SERVER_ERROR    | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9501      | SC_NOT_IMPLEMENTED          | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9502      | SC_BAD_GATEWAY              | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9503      | SC_SERVICE_UNAVAILABLE      | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9504      | SC_GATEWAY_TIMEOUT          | "status_code=500`component=Rackspace Identity Basic Auth`message=Failed with internal server error"
        9505      | SC_REQUEST_ENTITY_TOO_LARGE | "status_code=503`component=Rackspace Identity Basic Auth`message=Rate limited by identity service"
        9506      | SC_TOO_MANY_REQUESTS        | "status_code=503`component=Rackspace Identity Basic Auth`message=Rate limited by identity service"
    }
}
