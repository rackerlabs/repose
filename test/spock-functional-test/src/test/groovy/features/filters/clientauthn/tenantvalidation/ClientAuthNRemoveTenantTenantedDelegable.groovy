package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class ClientAuthNRemoveTenantTenantedDelegable extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenanteddelegable")
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

    @Unroll("tenant: #reqTenant isAuthed: #isAuthed isAdminAuthed: #isAdminAuthed")
    def "when authenticating user in tenanted and delegable mode and client-mapping matching - fail"() {

        given:

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = false
        fakeIdentityService.doesTenantHaveAdminRoles = false
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        System.out.println(mc)
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 0

        where:
        reqTenant | isAuthed | isAdminAuthed | responseCode | orphanedHandlings | cachedOrphanedHandlings
        111       | true     | false         | "500"        | 1                | 1
        555       | true     | true          | "401"        | 2                | 1
        666       | false    | true          | "401"        | 1                | 1
    }

    @Unroll("tenant: #reqTenant tenantMatch: #tenantMatch tenantWithAdmin")
    def "when authenticating user in tenanted and delegable mode and client-mapping matching - pass"() {

        given:

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        System.out.println(mc)
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.contains("x-auth-token")
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == "Confirmed"
        request2.headers.getFirstValue("x-authorization") == "Proxy " + reqTenant

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.contains("X-Default-Region")
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        mc.handlings[0].request.headers.contains("x-auth-token")
        mc.handlings[0].request.headers.contains("x-identity-status")
        mc.handlings[0].request.headers.contains("x-authorization")
        mc.handlings[0].request.headers.getFirstValue("x-identity-status") == "Confirmed"
        mc.handlings[0].request.headers.getFirstValue("x-authorization") == "Proxy " + reqTenant

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | orphanedHandlings | cachedOrphanedHandlings
        222       | true        | true                | 2                 | 0
        333       | true        | false               | 2                 | 0
        444       | false       | true                | 2                 | 1
    }

}
