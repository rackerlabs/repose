package features.filters.identitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
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

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    def "When the request has an HTTP Basic authentication header, then get a token and validate it"() {
        given: "basic auth"
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token")
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc.orphanedHandlings.size() == 4
    }

    def "No HTTP Basic authentication header sent and no token."() {
        when: "the request does not have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "simply pass it on down the filter chain and this configuration will respond with a SC_UNAUTHORIZED (401) and add an HTTP Basic authentication header"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        mc.orphanedHandlings.size() == 0
    }

    def "When the request has an x-auth-token, then still work with client-auth"() {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }
        def headers = ['X-Auth-Token': fakeIdentityService.client_token]

        when: "the request already has an x-auth-token header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        !mc.handlings[0].request.headers.getFirstValue(HttpHeaders.AUTHORIZATION)
        !mc.receivedResponse.headers.getFirstValue(HttpHeaders.WWW_AUTHENTICATE)
        mc.handlings[0].request.headers.getFirstValue("X-Auth-Token")
        mc.orphanedHandlings.size() == 2
    }

    def "When the request send with invalid key or username, then will fail to authenticate"() {
        given:
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic '+ Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.invalid_key).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "then get a token and validate it"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
        mc.orphanedHandlings.size() == 1
    }

    def "Ensure that subsequent calls within the cache timeout are retrieving the token from the cache"() {
        given:
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic '+ Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header"
        sleep(5000) //wait to make sure all cache the same token is time out
        MessageChain mc0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the cache"
        mc0.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc0.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc0.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc0.orphanedHandlings.size() == 1
        mc1.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc1.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc1.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.orphanedHandlings.size() == 0
    }

    def "Ensure that subsequent calls outside the cache timeout are retrieving a new token not from the cache"() {
        given:
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic '+ Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "multiple requests that have the same HTTP Basic authentication header"
        MessageChain mc0 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)
        sleep(6000)
        MessageChain mc1 = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "get the token from the cache"
        mc0.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc0.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc0.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc0.orphanedHandlings.size() == 1
        mc1.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc1.handlings[0].request.headers.getCountByName("X-Auth-Token") == 1
        mc1.handlings[0].request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)
        mc1.orphanedHandlings.size() == 1
    }

    @Unroll("Sending request with admin response set to HTTP #identityStatusCode")
    def "when failing to authenticate admin client"() {

        given:
        fakeIdentityService.with {
            tokenExpiresAt = DateTime.now().plusDays(1)
            generateTokenHandler = {
                request, xml ->
                    new Response(identityStatusCode, null, null, null)
            }
        }
        def headers = [
                'content-type'             : 'application/json',
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "User passes a request through repose"

        // TODO: This requires a delay for Deproxy MessageChain.orphanedHandlings()
        sleep 500

        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/servers/$reqTenant/", method: 'GET', headers: headers)

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == filterStatusCode.toString()
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1

        where:
        reqTenant | identityStatusCode                           | filterStatusCode
        9400      | HttpServletResponse.SC_BAD_REQUEST           | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9401      | HttpServletResponse.SC_UNAUTHORIZED          | HttpServletResponse.SC_UNAUTHORIZED
        9403      | HttpServletResponse.SC_FORBIDDEN             | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9404      | HttpServletResponse.SC_NOT_FOUND             | HttpServletResponse.SC_UNAUTHORIZED
        9500      | HttpServletResponse.SC_INTERNAL_SERVER_ERROR | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9501      | HttpServletResponse.SC_NOT_IMPLEMENTED       | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9502      | HttpServletResponse.SC_BAD_GATEWAY           | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9503      | HttpServletResponse.SC_SERVICE_UNAVAILABLE   | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        9504      | HttpServletResponse.SC_GATEWAY_TIMEOUT       | HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    def "When the Admin Token is not properly configured, then the response status code is SC_SERVICE_UNAVAILABLE (503)"() {
    }
}
