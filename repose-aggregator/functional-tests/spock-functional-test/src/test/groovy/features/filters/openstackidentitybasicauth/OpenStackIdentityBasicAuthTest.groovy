package features.filters.openstackidentitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

class OpenStackIdentityBasicAuthTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    def static ORIGIN_PASS_BODY = ":-)"
    def static ORIGIN_FAIL_BODY = "8^("

    def static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/openstackidentitybasicauth/common", params);
        repose.configurationProvider.applyConfigs("features/filters/openstackidentitybasicauth/standalone", params);

        repose.start()

        //TODO: The port finding logic is not working!
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service', null, { Request request -> return handleOriginRequest(request) })
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    def setup() {
        fakeIdentityService.client_apikey = MockIdentityService.DEFAULT_CLIENT_API_KEY
    }

    /**
     * Since there is no Auth-N/Auth-Z filter inline with the Basic Auth filter under test,
     * the origin service is simply making sure the AUTHORIZATION and/or X-AUTH-TOKEN headers are present with the
     * client information (e.g. User Name, API Key, Token) from the Mock Identity service.
     * @param request the HttpServletRequest from the "Client"
     * @return the HttpServletResponse from the "Origin"
     */
    def static Response handleOriginRequest(Request request) {
        // IF there is an Authorization header with the the
        if (request.headers.findAll("X-Auth-Token").contains(fakeIdentityService.client_token)) {
            return new Response(HttpServletResponse.SC_OK, null, null, ORIGIN_PASS_BODY)
        } else if (request.headers.findAll(HttpHeaders.USER_AGENT).contains(ORIGIN_PASS_BODY + ORIGIN_FAIL_BODY)) {
            def headers = [
                    (HttpHeaders.WWW_AUTHENTICATE): "Keystone realm=\"RAX-KEY\""
            ]
            return new Response(HttpServletResponse.SC_UNAUTHORIZED, null, headers, ORIGIN_FAIL_BODY)
        } else {
            return new Response(HttpServletResponse.SC_UNAUTHORIZED, null, null, ORIGIN_FAIL_BODY)
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

    def "No HTTP Basic authentication header sent."() {
        when: "the request does not have an HTTP Basic authentication header"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain and this configuration will respond with a SC_UNAUTHORIZED (401) and add an HTTP Basic authentication header"
        messageChain.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_FAIL_BODY)
        messageChain.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        messageChain.handlings.size() == 1
        messageChain.orphanedHandlings.size() == 0
        //messageChain.orphanedHandlings.empty // Slower than size() == 0
    }

    def "Don't touch the other WWW_AUTHENTICATE headers, just add the HTTP Basic authentication header."() {
        given: "the trigger header for our custom Origin Service Handler"
        def headers = [
                (HttpHeaders.USER_AGENT): (ORIGIN_PASS_BODY + ORIGIN_FAIL_BODY)
        ]

        when: "the request does not have any authentication header"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "simply pass it on down the filter chain and this configuration will respond with a SC_UNAUTHORIZED (401), add an HTTP Basic authentication header, and don't touch the Keystone header"
        messageChain.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_FAIL_BODY)
        messageChain.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Keystone realm=\"RAX-KEY\"")
        messageChain.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        messageChain.handlings.size() == 1
        messageChain.orphanedHandlings.size() == 0
        //messageChain.orphanedHandlings.empty // Slower than size() == 0
    }

    //@Ignore // TODO: This would require tight coupling with the other filters and monitoring the system model.
    def "Log a very loud WARNING stating the OpenStack Basic Auth filter cannot be used alone."() {
        expect: "check for the WARNING."
        reposeLogSearch.searchByString("WARNING").size() > 0
    }

    // NOTE: This would normally be removed by a Header Normalization filter.
    def "Request that already contains X-Auth-Token header sent."() {
        given: "the X-Auth-Token header containing the User Token"
        def headers = [
                "X-Auth-Token": fakeIdentityService.client_token
        ]

        when: "the request already has credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "simply pass it on down the filter chain and this configuration will respond with an SC_OK (200) and will NOT add an HTTP Basic authentication header"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.handlings.size() == 1
        messageChain.orphanedHandlings.size() == 0
        //messageChain.orphanedHandlings.empty // Slower than size() == 0
        !messageChain.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    // NOTE: This would normally be removed by a Header Normalization filter.
    def "Request that contains both an X-Auth-Token and HTTP Basic authentication header is sent."() {
        given: "the X-Auth-Token header containing the User Token and an HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                "X-Auth-Token"             : fakeIdentityService.client_token,
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request already has credentials"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "simply pass it on down the filter chain NOT processing the HTTP Basic authentication header and this configuration will respond with an SC_OK (200) and will NOT add an HTTP Basic authentication header"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.handlings.size() == 1
        messageChain.orphanedHandlings.size() == 0
        //messageChain.orphanedHandlings.empty // Slower than size() == 0
        !messageChain.receivedResponse.headers.findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "Retrieve a token for an HTTP Basic authentication header with UserName/ApiKey"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token for it"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        messageChain.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        messageChain.orphanedHandlings.size() == 1 // This is the call to the Mock Identity service through deproxy.
    }

    @Unroll("Sending request with invalid UserName #userName and API Key #apiKey pair.")
    def "Fail to retrieve a token for an HTTP Basic authentication header with an invalid UserName/ApiKey pair"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((userName + ":" + apiKey).bytes)
        ]
        fakeIdentityService.client_apikey = fakeIdentityService.invalid_key

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token for it"
        messageChain.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_FAIL_BODY)
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.headers.getCountByName("X-Auth-Token") == 0
        messageChain.orphanedHandlings.size() == 1 // This is the call to the Mock Identity service through deproxy.

        where:
        userName                            | apiKey
        fakeIdentityService.client_username | "BAD-API-KEY"
        "BAD-USER-NAME"                     | fakeIdentityService.client_apikey
        "BAD-USER-NAME"                     | "BAD-API-KEY"
    }

    @Ignore
    // Only the first AUTHORIZATION Basic header will be processed.
    def "Stop trying to retrieve a token for an HTTP Basic authentication header after a token has been obtained."() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + "BAD-API-KEY").bytes),
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes),
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString(("BAD-USER-NAME" + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header with UserName/ApiKey"
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token for it"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        messageChain.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        //messageChain.orphanedHandlings.size() == ??? // This indeterminate since the order of header processing is unknown.
    }

    def "Ensure that subsequent calls within the cache timeout are retrieving the token from the cache"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header"
        def messageChain0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the cache"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        messageChain.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        messageChain.orphanedHandlings.size() == 0
        //messageChain.orphanedHandlings.empty // Slower than size() == 0
    }

    def "Ensure that subsequent calls outside the cache timeout are retrieving a new token not from the cache"() {
        given: "the HTTP Basic authentication header containing the User Name and API Key"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header, but are separated by more than the cache timeout"
        def messageChain0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)
        sleep(5000) // How do I get this programmatically from the config.
        def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the Identity (Keystone) service"
        messageChain.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        messageChain.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        messageChain.handlings.size() == 1
        messageChain.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        messageChain.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        messageChain.orphanedHandlings.size() == 1
    }
}
