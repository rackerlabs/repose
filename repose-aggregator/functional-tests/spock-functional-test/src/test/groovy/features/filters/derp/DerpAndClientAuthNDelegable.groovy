package features.filters.derp

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jamesc on 12/1/14.
 */
class DerpAndClientAuthNDelegable extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/derp/responsemessaging/clientauthn", params)
        repose.start()
        waitUntilReadyToServiceRequests('401')

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        fakeIdentityService.resetHandlers()
    }

    /*
        These tests are to verify the delegation of authn failures to the derp filter, which then forwards
        that information back to the client.  The origin service, thus, never gets invoked.
    */

    @Unroll("tenant: #requestTenant, response: #responseTenant, and #delegatedMsg")
    def "when req without token, non tenanted and delegable mode with quality"() {
        given:
        fakeIdentityService.with {
            client_token = ""
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose with tenant in service admin role = $serviceAdminRole, request tenant: $requestTenant, response tenant: $responseTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.contains("Content-Type")
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0

        /* expected internal delegated message to derp from authn:
            "status_code=401.component=client-auth-n.message=Failure in AuthN filter.;q=0.3"
        */

        where:
        requestTenant | responseTenant  | serviceAdminRole  | reponseCode   | msgBody
        506           | 506             | "not-admin"       | "401"         | "Failure in AuthN filter"
        ""            | 512             | "not-admin"       | "401"         | "Failure in AuthN filter"
    }


    @Unroll ("Req with auth resp: #authRespCode")
    def "When req with invalid token using delegable mode with quality" () {
        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
        }

        fakeIdentityService.validateTokenHandler = {
            tokenId, request,xml ->
                new Response(authRespCode)
        }

        when: "User passes a request through repose expire/invalid token"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1234",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then:
        mc.receivedResponse.code == responseCode
        mc.receivedResponse.headers.contains("Content-Type")
        mc.receivedResponse.body.contains(msgBody)
        mc.handlings.size() == 0
        mc.getOrphanedHandlings().size() == 2

        /* expected internal delegated messages to derp from authn:
            "status_code=401.component=client-auth-n.message=Unable to validate token:\\s.*;q=0.3"
            "status_code=500.component=client-auth-n.message=Failure in AuthN filter.;q=0.3"
        */
        where:
        authRespCode | responseCode   | msgBody
        404          | "401"          | "Unable to validate token"
        401          | "500"          | "Failure in AuthN filter"
    }
}

