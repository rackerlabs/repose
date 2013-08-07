package features.filters.clientauthn.tenantvalidation

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.Handling
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response

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

    IdentityServiceResponseSimulator fakeIdentityService

    def setup() {
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')
    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }


    def "when passing in user's role and tenant id"() {

        given:
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService = new IdentityServiceResponseSimulator()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);

        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

        when: "User passes a request through repose"
        fakeIdentityService.validateTokenCount = 0
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['content-type':'application/json','X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        //fakeIdentityService.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"

        when: "User passes a request through repose"
        fakeIdentityService.validateTokenCount = 0
        mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        //fakeIdentityService.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.contains("X-Default-Region")
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"

    }

    def "when starting repose with invalid service-admin roles, fail"() {

    }

}
