package features.filters.identitybasicauth
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

class BasicAuthTest extends ReposeValveTest {

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

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service', null, { Request request -> return handleOriginRequest(request) })
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    /**
     * Since the is Auth-N filter is inline with the Basic Auth filter under test,
     * the origin service is simply making sure the X-AUTH-TOKEN header is present.
     * @param request the HttpServletRequest from the "Client"
     * @return the HttpServletResponse from the "Origin"
     */
    def static Response handleOriginRequest(Request request) {
        if (request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)) {
            return new Response(HttpServletResponse.SC_OK, null, null, BasicAuthStandaloneTest.ORIGIN_PASS_BODY)
        } else {
            return new Response(HttpServletResponse.SC_UNAUTHORIZED, null, null, BasicAuthStandaloneTest.ORIGIN_FAIL_BODY)
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
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain and this configuration will respond with a SC_UNAUTHORIZED (401) and add an HTTP Basic authentication header"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        mc.orphanedHandlings.size() == 0
    }

    def "When the request does have an HTTP Basic authentication header, then get a token and validate it"() {
        given:
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.handlings[0].request.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
        !mc.handlings[0].request.headers.contains("X-Auth-Token")
        mc.orphanedHandlings.size() == 0
    }

    def "When the request send with invalid key or username, then will fail to authenticate"() {
        given:
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        mc.orphanedHandlings.size() == 0
    }

    @Unroll("Sending request with admin response set to HTTP #adminResponseCode")
    def "when failing to authenticate admin client"() {

        given:
        fakeIdentityService.with {
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request, xml ->
                    new Response(adminResponseCode, null, null, responseBody)
            }
        }
        def headers = [
                'content-type': 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        where:
        reqTenant   | adminResponseCode | responseCode | responseBody                   | orphanedHandlings
        1111        | 500               | "500"        | ""                             | 1
        1112        | 404               | "500"        | ""                             | 1
        1113        | 401               | "401"        | ""                             | 1
    }

    def "When the request does have an x-auth-token, then still work with client-auth"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }
        def headers = ['X-Auth-Token': fakeIdentityService.client_token]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.handlings[0].request.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
        mc.handlings[0].request.headers.contains("X-Auth-Token")
        mc.orphanedHandlings.size() == 0
    }

    def "When the Admin Token is not properly configured, then the response status code is SC_SERVICE_UNAVAILABLE (503)"() {
    }
}
