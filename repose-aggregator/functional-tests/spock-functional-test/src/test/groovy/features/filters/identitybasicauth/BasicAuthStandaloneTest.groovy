package features.filters.identitybasicauth

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.apache.commons.codec.binary.Base64
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.HttpHeaders

import static framework.TestUtils.timedSearch

/**
 * Created by jennyvo on 9/17/14.
 * Basic Auth filter can't be used alone, have to use with client-auth filter
 */
class BasicAuthStandaloneTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint
    def static ORIGIN_PASS_BODY = ":-)"
    def static ORIGIN_FAIL_BODY = "8^("

    def static MockIdentityService fakeIdentityService

    //Start repose once for this particular translation test
    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth", params);
        repose.configurationProvider.applyConfigs("features/filters/identitybasicauth/onlybasicauth", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service', null, { Request request -> return handleOriginRequest(request) })
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityService.handler)
    }

    /**
     * Since there is no Auth-N/Auth-Z filter inline with the Basic Auth filter under test,
     * the origin service is simply making sure the AUTHORIZATION and/or X-AUTH-TOKEN headers are present with the
     * client information (e.g. User Name, API Key, Token) from the Mock Identity service.
     * @param request the HttpServletRequest from the "Client"
     * @return the HttpServletResponse from the "Origin"
     */
    def static Response handleOriginRequest(Request request) {
        if (request.getHeaders().getFirstValue(HttpHeaders.AUTHORIZATION).equals(
                'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)) ||
                request.headers.getFirstValue("X-Auth-Token").equals(fakeIdentityService.client_token)) {
            return new Response(HttpServletResponse.SC_OK, null, null, ORIGIN_PASS_BODY)
        } else {
            return new Response(HttpServletResponse.SC_UNAUTHORIZED, null, null, ORIGIN_FAIL_BODY)
        }
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "when start repose with basic auth in filter without client-auth" () {
        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint])

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.receivedResponse.body.equals(ORIGIN_FAIL_BODY)
        mc.handlings.size() == 1
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "when start repose with x-auth-token, basicauth shouldn't work" () {
        given:
        def headers = [
                "X-Auth-Token": fakeIdentityService.client_token
        ]

        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        mc.handlings.size() == 1
        !mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    def "when send request with credential" () {
        def headers = [
                (HttpHeaders.AUTHORIZATION): 'Basic ' + Base64.encodeBase64URLSafeString((fakeIdentityService.client_username + ":" + fakeIdentityService.client_apikey).bytes)
        ]

        when: "the request does have an HTTP Basic authentication header"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.receivedResponse.body.equals(ORIGIN_PASS_BODY)
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("X-Auth-Token")
        mc.orphanedHandlings.size() == 1 // This is the call to the Mock Identity service through deproxy.
    }

    //@Ignore // TODO: This would require tight coupling with the other filters and monitoring the system model.
    def "Log a very loud WARNING stating the OpenStack Basic Auth filter cannot be used alone."() {
        expect: "check for the WARNING."
        reposeLogSearch.searchByString("WARNING").size()  > 0
    }
}
