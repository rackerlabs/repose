package features.filters.clientauthn.tenantvalidation
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class ClientAuthNTenantedDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/tenanteddelegable", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup(){
        fakeIdentityService.resetHandlers()
    }


    @Unroll("tenant: #requestTenant, with return from identity with HTTP code (#authResponseCode) response tenant: #responseTenant, token: #clientToken")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching - fail"() {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = "not-admin"
        }

        if(authResponseCode != 200){
            fakeIdentityService.validateTokenHandler = {
                tokenId, request,xml ->
                    new Response(authResponseCode)
            }
        }


        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        requestTenant | responseTenant  | authResponseCode | responseCode | clientToken
        300           | 301             | 500              | "500"        | UUID.randomUUID()
        302           | 303             | 404              | "401"        | UUID.randomUUID()
        304           | 305             | 200              | "401"        | UUID.randomUUID()
        306           | 306             | 200              | "401"        | ""

    }


    @Unroll("tenant: #requestTenant, with return from identity with response tenant: #responseTenant, token: #clientToken, and role: #serviceAdminRole")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching - pass"() {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        and: "If request made it to origin service"
        if (identityStatus == "Confirmed") {
            def request2 = mc.handlings[0].request
            assert(mc.handlings[0].endpoint == originEndpoint)
            assert(request2.headers.contains("x-auth-token"))
            assert(request2.headers.contains("x-identity-status"))
            assert(request2.headers.contains("x-authorization"))
            assert(request2.headers.getFirstValue("x-identity-status") == identityStatus)
            assert(request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        and: "If identityStatus was Indeterminate"
        if (identityStatus == "Indeterminate") {

            def request2 = mc.handlings[0].request
            assert(request2.headers.getFirstValue("x-identity-status") == identityStatus)
            assert(request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        where:
        requestTenant | responseTenant  | serviceAdminRole      | responseCode | identityStatus  | clientToken
        307           | 307             | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID()
        308           | 308             | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()
        309           | 310             | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()
        ""            | 312             | "not-admin"           | "200"        | "Indeterminate" | ""
    }

}
