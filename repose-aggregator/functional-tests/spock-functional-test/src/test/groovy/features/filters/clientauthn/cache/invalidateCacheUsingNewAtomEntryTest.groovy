package features.filters.clientauthn.cache

import features.filters.clientauthn.AtomFeedResponseSimulator
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 2/12/15.
 */
class invalidateCacheUsingNewAtomEntryTest extends ReposeValveTest {

    def originEndpoint
    def identityEndpoint
    def atomEndpoint

    MockIdentityService fakeIdentityService
    AtomFeedResponseSimulator fakeAtomFeed

    def setup() {
        deproxy = new Deproxy()

        int atomPort = properties.atomPort
        fakeAtomFeed = new AtomFeedResponseSimulator(atomPort)
        atomEndpoint = deproxy.addEndpoint(atomPort, 'atom service', null, fakeAtomFeed.handler)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/atom", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort,'origin service')

        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

    }

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    def "verify also support new TRR_User event"() {
        when: "I send a GET request to REPOSE with an X-Auth-Token header"
        fakeIdentityService.resetCounts()
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then: "REPOSE should validate the token and then pass the request to the origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1

        //Repose is getting an admin token and groups, so the number of
        //orphaned handlings doesn't necessarily equal the number of times a
        //token gets validated
        fakeIdentityService.validateTokenCount == 1
        mc.handlings[0].endpoint == originEndpoint


        when: "I send a GET request to REPOSE with the same X-Auth-Token header"
        fakeIdentityService.resetCounts()
        mc = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Repose should use the cache, not call out to the fake identity service, and pass the request to origin service"
        mc.receivedResponse.code == '200'
        mc.handlings.size() == 1
        fakeIdentityService.validateTokenCount == 0
        mc.handlings[0].endpoint == originEndpoint


        when: "identity atom feed has an entry that should invalidate the tenant associated with this X-Auth-Token"
        // change identity atom feed

        fakeIdentityService.with {
            fakeIdentityService.validateTokenHandler = {
                tokenId, request, xml ->
                    new Response(404)
            }
        }
        fakeIdentityService.resetCounts()
        fakeAtomFeed.hasEntry = true
        fakeAtomFeed.isUserEntry = true
        atomEndpoint.defaultHandler = fakeAtomFeed.handler



        and: "we sleep for 11 seconds so that repose can check the atom feed"
        sleep(15000)

        and: "I send a GET request to REPOSE with the same X-Auth-Token header"
        mc = deproxy.makeRequest(
                [
                        url           : reposeEndpoint,
                        method        : 'GET',
                        headers       : ['X-Auth-Token': fakeIdentityService.client_token],
                        defaultHandler: fakeIdentityService.handler
                ])

        then: "Repose should not have the token in the cache any more, so it try to validate it, which will fail and result in a 401"
        mc.receivedResponse.code == '401'
        mc.handlings.size() == 0
        fakeIdentityService.validateTokenCount == 1
    }
}
