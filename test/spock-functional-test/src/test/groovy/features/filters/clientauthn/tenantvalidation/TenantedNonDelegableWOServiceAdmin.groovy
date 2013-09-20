package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class TenantedNonDelegableWOServiceAdmin extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/noserviceroles")
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
    def "when authenticating user in tenanted and non delegable mode and without service-admin - fail"() {

        given:


        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
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
        mc.orphanedHandlings.size() == 1
        mc.handlings.size() == 0

        where:
        reqTenant | tenantMatch | isAuthed | isAdminAuthed | responseCode | orphanedHandlings
        111       | false       | true     | false         | "500"        | 1
        333       | false       | true     | true          | "401"        | 2
        444       | true        | false    | true          | "401"        | 1
        555       | false       | false    | true          | "401"        | 1


    }

    def "when authenticating user in tenanted and non delegable mode and without service-admin - pass"() {

        given:


        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = true
        fakeIdentityService.doesTenantHaveAdminRoles = false
        fakeIdentityService.client_tenant = 222
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + 222 + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        System.out.println(mc)
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 2
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.contains("x-auth-token")
        !request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-authorization") == "Proxy " + 222

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + 222 + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
    }

}
