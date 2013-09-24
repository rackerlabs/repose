package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

class ClientAuthNTenantedDelegableTest extends ReposeValveTest {

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

    @Unroll("token: #clientToken")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching - fail"() {

        // def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed


        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = false
        fakeIdentityService.doesTenantHaveAdminRoles = false
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/$reqTenant", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        where:
        reqTenant | isAuthed | isAdminAuthed | responseCode | orphanedHandlings | clientToken

        128       | true     | false         | "500"        | 1                 | UUID.randomUUID()
        126       | true     | true          | "401"        | 2                 | UUID.randomUUID()
        127       | false    | true          | "401"        | 1                 | UUID.randomUUID()
        129       | false    | true          | "401"        | 0                 | ""

    }

    @Unroll("tenantMatch: #tenantMatch tenantWithAdmin: token: #clientToken")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching - pass"() {

        // def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = isAdminAuthed


        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/$reqTenant", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == orphanedHandlings

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
        reqTenant | tenantMatch | tenantWithAdminRole | isAdminAuthed | orphanedHandlings | identityStatus  | clientToken

        123       | true        | true                | true          | 2                 | "Confirmed"     | UUID.randomUUID()
        124       | true        | false               | true          | 2                 | "Confirmed"     | UUID.randomUUID()
        125       | false       | true                | true          | 2                 | "Confirmed"     | UUID.randomUUID()
        ""        | false       | false               | false         | 0                 | "Indeterminate" | ""

    }

}
