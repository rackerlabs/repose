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
//
//    def cleanup() {
//        deproxy.shutdown()
//    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    @Unroll("tenantMatch: #tenantMatch tenantWithAdmin: #tenantWithAdminRole isAuthed: #isAuthed isAdminAuthed: #isAdminAuthed token: #clientToken")
    def "when authenticating user in tenanted and delegable mode and client-mapping not matching"() {

        // def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed


        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/$reqTenant", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings

        and: "If request made it to origin service"
        if (identityStatus == "Confirmed") {
            def request2 = mc.handlings[0].request
            mc.handlings[0].endpoint == originEndpoint
            request2.headers.contains("x-auth-token")
            request2.headers.contains("x-identity-status")
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("x-identity-status") == identityStatus
            request2.headers.getFirstValue("x-authorization").startsWith("Proxy")
        }

        and: "If identityStatus was Indeterminate"
        if (identityStatus == "Indeterminate") {

            def request2 = mc.handlings[0].request
            request2.headers.getFirstValue("x-identity-status") == identityStatus
            request2.headers.getFirstValue("x-authorization").startsWith("Proxy")
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | isAdminAuthed | responseCode | handlings | orphanedHandlings | identityStatus  | clientToken

        128       | false       | false               | true     | false         | "500"        | 0         | 1                 | ""              | UUID.randomUUID()
        123       | true        | true                | true     | true          | "200"        | 1         | 3                 | "Confirmed"     | UUID.randomUUID()
        124       | true        | false               | true     | true          | "200"        | 1         | 2                 | "Confirmed"     | UUID.randomUUID()
        125       | false       | true                | true     | true          | "200"        | 1         | 2                 | "Confirmed"     | UUID.randomUUID()
        126       | false       | false               | true     | true          | "401"        | 0         | 1                 | ""              | UUID.randomUUID()
        127       | false       | false               | false    | true          | "401"        | 0         | 1                 | ""              | UUID.randomUUID()
        ""        | false       | false               | true     | false         | "200"        | 1         | 0                 | "Indeterminate" | ""
        129       | false       | false               | false    | true          | "401"        | 0         | 0                 | ""              | ""

    }

}
