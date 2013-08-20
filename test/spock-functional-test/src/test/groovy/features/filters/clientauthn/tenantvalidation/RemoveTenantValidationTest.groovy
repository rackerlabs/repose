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

/**
 Service-admin users do not have tenant validation performed in Client-AuthN filter.

 RBAC MVP teams have had their RBAC configs have been revisited with the service-admin user mods suggested to the clients.  RBAC MVP customers are:

 NextGen (not repose RBAC)
 FirstGen
 CloudFiles
 CloudDBaaS
 CloudLBaaS

 If running client-auth in tenanted mode, x-tenant = tenant id from URI.

 Racker users are not allowed through. (regression - they weren't allowed before, don't expose a hole that allows them in now).  Rackers do not have tenants therefore they still won't come through.


 Test Cases:

 B-52709
 ASSUMPTION is that regression suite still passes (may be an incorrect assumption since we are removing belongsTo)
 user with correct role and correct tenant
    - returns identitySuccessXmlWithServiceAdminTemplate for token (that returns service-admin role)
    - returns http 200
    - verified that belongsTo call is not made in orphanhandlings
 User without correct role and correct tenant
    - returns identitySuccessXmlTemplate for token
    - returns http 200
    - verified that belongsTo call is made in orphanhandlings
 User with invalid tenant
    - returns identityFailureXmlTemplate
    - returns http 401
 User with correct role with racker tenant
    - returns identitySuccessXmlWithServiceAdminRackerTemplate for token (that returns service-admin role and racker account [no template])
    - returns http 200
    - verified that belongsTo call is made in orphanhandlings
 Client-auth-n does not have a valid role in service-admin roles – Repose does not start – xsd error
 Client-auth-n has multiple roles roles in service-admin roles element
    - repeat steps 1-4 for every role
 Repeat steps 1-4 for 2 iterations.  Validate that 2nd iteration did not go to identity but came from cache
*/

class RemoveTenantValidationTest extends ReposeValveTest{

    def originEndpoint
    def identityEndpoint

    IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setup() {
    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }


    def "when authenticating user in tenanted and delegable mode and client-mapping matching"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenanteddelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        System.out.println(mc)
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if(mc.handlings.size() > 0){
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
        mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }
        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        123       | true        | true                | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | true        | false               | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | false       | true                | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | true     | "401"        | 0         | 2                 | 1                       | 0
        123       | false       | false               | false    | "500"        | 0         | 1                 | 1                       | 0


    }

    def "when authenticating user in tenanted and delegable mode and client-mapping not matching"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenanteddelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-auth-token")
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == "Indeterminate"
        request2.headers.getFirstValue("x-authorization") == "Proxy"

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings
        123       | true        | true                | true     | "200"        | 1         | 0
        123       | true        | false               | true     | "200"        | 1         | 0
        123       | false       | true                | true     | "200"        | 1         | 0
        123       | false       | false               | true     | "200"        | 1         | 0
        123       | false       | false               | false    | "200"        | 1         | 0

    }

    def "when authenticating user in tenanted and non delegable mode"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenantednondelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        123       | true        | true                | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | true        | false               | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | false       | true                | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | true     | "401"        | 0         | 2                 | 1                       | 0
        123       | false       | false               | false    | "500"        | 0         | 1                 | 1                       | 0
    }

    def "when authenticating user in non tenanted and delegable mode with client-mapping matching"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/nontenanteddelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
            request2.headers.contains("x-auth-token")
            request2.headers.contains("x-identity-status")
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("x-identity-status") == "Confirmed"
            request2.headers.getFirstValue("x-authorization") == "Proxy"
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        123       | true        | true                | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | true        | false               | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | false       | true                | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | false    | "200"        | 1         | 1                 | 1                       | 1
    }

    def "when authenticating user in non tenanted and delegable mode with client-mapping not matching"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/nontenanteddelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
            request2.headers.contains("x-identity-status")
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("x-identity-status") == "Confirmed"
            request2.headers.getFirstValue("x-authorization") == "Proxy"
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        123       | true        | true                | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | true        | false               | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | false       | true                | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | false    | "200"        | 1         | 1                 | 1                       | 1

    }

    def "when authenticating user in non tenanted and non delegable mode"() {

        given:
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/nontenantednondelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        MessageChain mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint  + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if(mc.handlings.size() > 0){
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings
        123       | true        | true                | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | true        | false               | true     | "200"        | 1         | 3                 | 0                       | 1
        123       | false       | true                | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | true     | "200"        | 1         | 3                 | 1                       | 1
        123       | false       | false               | false    | "500"        | 0         | 1                 | 1                       | 0

    }
}
