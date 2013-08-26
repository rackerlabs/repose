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
    def "when authenticating user in tenanted and non delegable mode and without service-admin"() {

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
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
            request2.headers.contains("x-auth-token")
            request2.headers.contains("x-identity-status")
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("x-identity-status") == "Confirmed"
            request2.headers.getFirstValue("x-authorization") == "Proxy " + reqTenant
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }
        where:
        reqTenant | tenantMatch | isAuthed | isAdminAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        111       | false       | true     | false         | "500"        | 0         | 1                 | 1                       | 0
        222       | true        | true     | true          | "200"        | 1         | 3                 | 0                       | 1
        333       | false       | true     | true          | "401"        | 0         | 1                 | 1                       | 0
        444       | true        | false    | true          | "401"        | 0         | 1                 | 1                       | 0
        555       | false       | false    | true          | "401"        | 0         | 1                 | 1                       | 0


    }


}
