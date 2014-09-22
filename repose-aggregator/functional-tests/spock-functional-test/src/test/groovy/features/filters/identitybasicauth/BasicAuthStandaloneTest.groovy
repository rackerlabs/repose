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
/**
 * Created by jennyvo on 9/17/14.
 * Basic Auth filter can't be used alone, have to use with client-auth filter
 */
class BasicAuthStandaloneTest extends ReposeValveTest {
    def static originEndpoint
    def static identityEndpoint

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
     * the origin service is simply making sure the X-AUTH-TOKEN headers are present with the
     * User Token from the Mock Identity service.
     * @param request the HttpServletRequest from the "Client"
     * @return the HttpServletResponse from the "Origin"
     */
    def static Response handleOriginRequest(Request request) {
        // IF there is an Authorization header with the Client Token,
        // THEN return a Response with Status Code OK (200);
        // ELSE return a Response with Status Code UNAUTHORIZED (401) AND add a Keystone header.
        if (request.headers.findAll("X-Auth-Token").contains(fakeIdentityService.client_token)) {
            return new Response(HttpServletResponse.SC_OK, null, null, null)
        } else {
            def headers = [
                    (HttpHeaders.WWW_AUTHENTICATE): ("Keystone uri=localhost")
            ]
            return new Response(HttpServletResponse.SC_UNAUTHORIZED, null, headers, null)
        }
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "when start repose with basic auth, send request without credential" () {
        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET')

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_UNAUTHORIZED.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
        mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    // NOTE: This would normally be removed by a Header Normalization filter.
    def "when start repose with basic auth only, x-auth-token should work" () {
        given:
        def headers = [
                "X-Auth-Token": fakeIdentityService.client_token
        ]

        when: "send request without credential"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: headers)

        then: "request should pass as no basic auth filter"
        mc.receivedResponse.code == HttpServletResponse.SC_OK.toString()
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 0
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
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(HttpHeaders.AUTHORIZATION)
        mc.handlings[0].request.headers.contains("X-Auth-Token")
        mc.orphanedHandlings.size() == 1 // This is the call to the Mock Identity service through deproxy.
        !mc.receivedResponse.getHeaders().findAll(HttpHeaders.WWW_AUTHENTICATE).contains("Basic realm=\"RAX-KEY\"")
    }

    //@Ignore // TODO: This would require tight coupling with the other filters and monitoring the system model.
    def "Log a very loud WARNING stating the OpenStack Basic Auth filter cannot be used alone."() {
        expect: "check for the WARNING."
        reposeLogSearch.searchByString("WARNING").size()  > 0
    }
}
