
package features.filters.clientauthn.regionheader;

import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
B-50304
Pass region in header

Description:
    As the origin service, we want the users default region to be passed back 
    from the validate token response in the Auth component so that we know the 
    users default region.

Acceptance Criteria:
    The users default region is passed as header name x-default-region in the 
    header.  This value comes from rax-auth:default-region from the validate 
    token response.

Test Plan
    1. Use a simple mock origin service that returns 200 for all requests, and 
    the mock identity service to validate tokens and return a pre-determined 
    default region for the user associated with the token.
    2. Make a call to Repose with the test token. Check that:
        a. The mock identity service received a single validate-token request.
        b. The handlings list has one entry, indicating that the request 
           reached the origin service
        c. The request on that handling has a header named "X-Default-Region" 
           having as its value the default region returned by the mock identity 
           service
    3. Make a second call to Repose with the same token (should be cached). 
       Check that:
        a. The mock identity service did not receive any validate-token 
           requests, since the token info should be cached.
        b. The handlings list has one entry, indicating that the request 
           reached the origin service
        c. The request on that handling has a header named "X-Default-Region" 
           having as its value the default region returned by the mock identity 
           service
 */

class PassClientDefaulRegionInHeaderTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint

    IdentityServiceResponseSimulator fakeIdentityService

    def setup() {
        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/regionheader",
                "features/filters/clientauthn/connectionpooling")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(),'origin service')

        fakeIdentityService = new IdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)

    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    def "when a token is validated, should pass the default region as X-Default-Region"() {

        when: "I send a GET request to Repose with an X-Auth-Token header"
        fakeIdentityService.validateTokenCount = 0
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Repose should validate the token and path the user's default region as the X-Default_Region header to the origin service"
        mc.receivedResponse.code == "200"
        fakeIdentityService.validateTokenCount == 1
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request = mc.handlings[0].request
        request.headers.contains("X-Default-Region")
        request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        
        when: "I send a second GET request to Repose with the same token"
        fakeIdentityService.validateTokenCount = 0
        mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])
        
        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service with the same X-Default-Region header"
        mc.receivedResponse.code == "200"
        fakeIdentityService.validateTokenCount == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("X-Default-Region")
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"

    }
}
