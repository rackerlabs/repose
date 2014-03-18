package features.filters.clientauthn.tenantvalidation

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class NonTenantedNonDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/removetenant/nontenantednondelegable", params)
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
    def "when authenticating user in non tenanted and non delegable mode - fail"() {

        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = serviceAdminRole
            client_userid = requestTenant
        }

        if(authResponseCode != 200){
            fakeIdentityService.validateTokenHandler = {
                tokenId, request,xml ->
                    new Response(authResponseCode)
            }
        }

        if(groupResponseCode != 200){
            fakeIdentityService.getGroupsHandler = {
                userId, request,xml ->
                    new Response(groupResponseCode)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request should not be passed from repose"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        requestTenant | responseTenant  | serviceAdminRole      | authResponseCode | responseCode | groupResponseCode | clientToken
        613           | 613             | "not-admin"           | 500              | "500"        | 200               | UUID.randomUUID()
        614           | 614             | "not-admin"           | 404              | "401"        | 200               | UUID.randomUUID()
        615           | 615             | "not-admin"           | 200              | "500"        | 404               | UUID.randomUUID()
        616           | 616             | "not-admin"           | 200              | "500"        | 500               | UUID.randomUUID()
        ""            | 612             | "not-admin"           | 200              | "500"        | 200               | ""
    }

    @Unroll("Tenant: #requestTenant")
    def "when authenticating user in non tenanted and non delegable mode - pass"() {

        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            service_admin_role = serviceAdminRole
            client_userid = requestTenant
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant/",
                method: 'GET',
                headers: [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == default_region

        where:
        requestTenant | responseTenant  | serviceAdminRole      | clientToken        | default_region
        604           | 605             | "not-admin"           | UUID.randomUUID()  | "the-default-region"
        607           | 607             | "not-admin"           | UUID.randomUUID()  | "the-default-region"
        608           | 608             | "service:admin-role1" | UUID.randomUUID()  | "the-default-region"
        609           | 610             | "service:admin-role1" | UUID.randomUUID()  | "the-default-region"
    }

}
