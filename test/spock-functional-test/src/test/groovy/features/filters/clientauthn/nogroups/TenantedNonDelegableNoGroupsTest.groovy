package features.filters.clientauthn.nogroups

import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import spock.lang.Unroll

class TenantedNonDelegableNoGroupsTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint
    def static Map<String,String> headersCommon = [
            'X-Default-Region':'the-default-region',
            'x-auth-token':'token',
            'x-forwarded-for':'127.0.0.1',
            'x-pp-user': 'username;q=1.0'
    ]


    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/nogroups")
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
    def "when authenticating user in tenanted and non delegable mode - fail scenarios"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        fakeIdentityService.isValidateClientTokenBroken = validateClientBroken
        fakeIdentityService.isGetAdminTokenBroken = getAdminTokenBroken
        fakeIdentityService.isGetGroupsBroken = getGroupsBroken
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        mc.receivedResponse.headers.contains("www-authenticate") == x_www_auth

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == 1
        mc.handlings.size() == 0

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | isAdminAuthed | responseCode | orphanedHandlings | x_www_auth  |validateClientBroken | getAdminTokenBroken | getGroupsBroken
        111       | false       | false               | true     | false         | "500"        | 1                 | false       | false               | false               | false
        888       | true        | true                | true     | true          | "500"        | 1                 | false       | false               | true                | false
        555       | false       | false               | true     | true          | "401"        | 2                 | true        | false               | false               | false
        666       | false       | false               | false    | true          | "401"        | 1                 | true        | false               | false               | false
        777       | true        | true                | true     | true          | "500"        | 1                 | false       | true                | false               | false
    }


    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in tenanted and non delegable mode - success"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        fakeIdentityService.isValidateClientTokenBroken = validateClientBroken
        fakeIdentityService.isGetAdminTokenBroken = getAdminTokenBroken
        fakeIdentityService.isGetGroupsBroken = getGroupsBroken
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-tenant-name") == (tenantMatch ? reqTenant.toString() : "9999999")
        request2.headers.contains("x-token-expires")
        request2.headers.getFirstValue("x-pp-user") == "username;q=1.0"
        request2.headers.contains("x-roles")
        request2.headers.getFirstValue("x-authorization") == "Proxy " + reqTenant
        request2.headers.getFirstValue("x-user-name") == "username"
        !request2.headers.contains("x-pp-groups")

        mc.receivedResponse.headers.contains("www-authenticate") == false

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        mc.handlings[0].request.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        mc.handlings[0].request.headers.getFirstValue("x-tenant-name") == (tenantMatch ? reqTenant.toString() : "9999999")
        mc.handlings[0].request.headers.getFirstValue("x-pp-user") == "username;q=1.0"
        mc.handlings[0].request.headers.contains("x-token-expires")
        mc.handlings[0].request.headers.contains("x-roles")
        mc.handlings[0].request.headers.getFirstValue("x-authorization") == "Proxy " + reqTenant
        mc.handlings[0].request.headers.getFirstValue("x-user-name") == "username"
        !mc.handlings[0].request.headers.contains("x-pp-groups")

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | responseCode | orphanedHandlings | cachedOrphanedHandlings | x_pp_groups |validateClientBroken | getAdminTokenBroken | getGroupsBroken
        222       | true        | true                | "200"        | 1                 | 0                       | false | false                | false               | false
        333       | true        | false               | "200"        | 1                 | 0                       | false | false                | false               | false
        444       | false       | true                | "200"        | 1                 | 1                       | false | false                | false               | false
        100       | true        | true                | "200"        | 1                 | 0                       | false | false                | false               | true
    }
}
