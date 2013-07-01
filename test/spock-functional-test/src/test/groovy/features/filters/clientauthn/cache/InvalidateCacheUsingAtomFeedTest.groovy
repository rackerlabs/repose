package features.filters.clientauthn.cache
import features.filters.clientauthn.AtomFeedResponseSimulator
import features.filters.clientauthn.IdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
/**
 B-48277
 Use the Identity Atom Feed to Clear Deleted, Disabled, and Revoked Tokens from Cache
 https://www15.v1host.com/RACKSPCE/story.mvc/Summary?oidToken=Story%3A639030

 Test Plan

 This test will allow us to ensure that Repose is properly responding to events
 in the identity feed. It will not require creation of test accounts, or even
 interaction with the staging or prod identity system. It is entirely
 self-contained, and uses Deproxy as fake service endpoint to return
 pre-determined responses to Repose.

 Still neeed to sort out:
 how to configure the atom-reading part (e.g. tell it the url of the feed)
 The exact format of the responses that the fake services will return
 fake identity - token is good, token is bad
 fake atom - empty feed, feed with one entry, maybe empty feed again

 This test involves 5 objects:
 Client
 Repose
 Origin service
 Identity service
 Atom (Hopper) service

 Deproxy will take the place of the client and all three services

 configure repose (in no particular order):
 auth against the identity service
 look for identity updates from the atom service
 forward requests to origin service

 create endpoints
 default responses:
 fake identity service returns positive response to auth requests
 fake atom service returns empty feed, with appropriate next and/or previous
 links
 origin service returns 200

 procedure:

 request #1 - send a request to repose with some token for a fake user
 fake identity will say token is good
 repose will cache the token
 repose will forward the request to the origin service
 assert fake identity received one request (probably orphaned, that is,
 without a Deproxy-Request-Id header)
 assert origin service received one request

 request #2 - send a request with the same token as above
 repose will find the token in the cache, and thus not talk to identity
 repose will forward the request to origin service
 assert fake identity received zero requests
 assert origin service received one request

 tell fake atom feed to start showing a single atom entry that invalidates the
 token

 repose will, at some point, read the updated feed and should invalidate the
 token and remove it from the cache
 upon receiving the request for the feed from repose and returning the
 single entry, fake atom service should switch itself to displaying an empty
 feed

 request #3 - send a request with the same token
 repose won't have it in the cache, so it will ask fake identity
 fake identity will return a negative response (404, I think)
 repose will return an error (401, I think) and not forward the request to
 the origin service
 assert fake identity received one request
 assert origin service received zero requests
 assert response code from repose is 401

 */
class InvalidateCacheUsingAtomFeedTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint
    def atomEndpoint

    IdentityServiceResponseSimulator fakeIdentityService
    AtomFeedResponseSimulator fakeAtomFeed

    def setup() {
        deproxy = new Deproxy()

        int atomPort = properties.getProperty("atom.port").toInteger()
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        repose.applyConfigs("features/filters/clientauthn/common", "features/filters/clientauthn/atom")
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

    def "when token is cached then invalidated by atom feed, should attempt to revalidate token with identity endpoint"() {

        when: "I send a GET request to REPOSE with an X-Auth-Token header"
        fakeIdentityService.validateTokenCount = 0
        MessageChain mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1

        //Repose is getting an admin token and groups, so the number of
        //orphaned handlings doesn't necessarily equal the number of times a
        //token gets validated
        fakeIdentityService.validateTokenCount == 1
        mc.handlings[0].endpoint == originEndpoint


        when:"I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeIdentityService.validateTokenCount = 0
        mc = deproxy.makeRequest(reposeEndpoint, 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityService.validateTokenCount == 0
        mc.handlings[0].endpoint == originEndpoint


        when: "identity atom feed has an entry that should invalidate the tenant associated with this X-Auth-Token"
        // change identity atom feed
        fakeIdentityService.ok = false
        fakeIdentityService.validateTokenCount = 0
        fakeAtomFeed.hasEntry = true
        atomEndpoint._defaultHandler = fakeAtomFeed.handler

        and: "we sleep for 11 seconds so that repose can check the atom feed"
        sleep(15000)

        and: "I send a GET request to REPOSE with the same X-Auth-Token header"
        mc = deproxy.makeRequest(
                [
                        url:reposeEndpoint,
                        method:'GET',
                        headers:['X-Auth-Token': fakeIdentityService.client_token],
                        defaultHandler:fakeIdentityService.handler
                ])

        then: "Repose should not have the token in the cache any more, so it try to validate it, which will fail and result in a 401"
        mc.receivedResponse.code == '401'
        mc.handlings.size() == 0
        fakeIdentityService.validateTokenCount == 1
    }

}
