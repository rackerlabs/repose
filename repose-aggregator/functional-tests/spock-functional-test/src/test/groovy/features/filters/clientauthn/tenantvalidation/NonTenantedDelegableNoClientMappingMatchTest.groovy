package features.filters.clientauthn.tenantvalidation

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class NonTenantedDelegableNoClientMappingMatchTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/nontenanteddelegable", params)
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


    @Unroll("Tenant: #requestTenant")
    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching - fail"() {
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        if(authResponseCode != 200){
            fakeIdentityService.validateTokenHandler = {
                tokenId, request,xml ->
                    new Response(authResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint//servers/$requestTenant/",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        requestTenant | responseTenant  | serviceAdminRole      | authResponseCode | responseCode
        400           | 401             | "not-admin"           | 500              | "500"
        402           | 403             | "not-admin"           | 404              | "401"

    }


    @Unroll("Tenant: #requestTenant")
    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching - pass"() {

        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint//servers/$requestTenant/",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == default_region
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == identityStatus
        request2.headers.getFirstValue("x-authorization") == "Proxy"

        where:
        requestTenant | responseTenant  | serviceAdminRole      | responseCode | identityStatus  | clientToken        | default_region
        404           | 405             | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID()  | "the-default-region"
        406           | 406             | "not-admin"           | "200"        | "Indeterminate" | ""                 | null
        407           | 407             | "not-admin"           | "200"        | "Confirmed"     | UUID.randomUUID()  | "the-default-region"
        408           | 408             | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()  | "the-default-region"
        409           | 410             | "service:admin-role1" | "200"        | "Confirmed"     | UUID.randomUUID()  | "the-default-region"
        ""            | 412             | "not-admin"           | "200"        | "Indeterminate" | ""                 | null

    }


}
