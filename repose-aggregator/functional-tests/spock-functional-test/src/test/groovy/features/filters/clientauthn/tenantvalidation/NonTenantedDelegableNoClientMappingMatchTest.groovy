package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class NonTenantedDelegableNoClientMappingMatchTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/nontenanteddelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }


    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching - fail"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed
        fakeIdentityService.client_userid =reqTenant

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = false
        fakeIdentityService.doesTenantHaveAdminRoles = false
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/" + reqTenant + "/", method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 0

        where:
        reqTenant | isAuthed | isAdminAuthed | responseCode | orphanedHandlings | cachedOrphanedHandlings
        111       | true     | false         | "500"        | 1                 | 1
        666       | false    | true          | "401"        | 2                 | 0

    }

    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching - pass"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true
        fakeIdentityService.client_userid =reqTenant

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method:'GET', headers:['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == "Confirmed"
        request2.headers.getFirstValue("x-authorization") == "Proxy"

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(url:reposeEndpoint + "/servers/" + reqTenant + "/", method:'GET', headers:['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | orphanedHandlings | cachedOrphanedHandlings
        222       | true        | true                | 2                 | 0
        333       | true        | false               | 2                 | 0
        444       | false       | true                | 2                 | 0
        555       | false       | false               | 1                 | 0

    }


}
