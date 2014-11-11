package features.filters.clientauthz.serviceresponse

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/11/14.
 * check delegating option for authz
 */
class ClientAuthZDelegatingTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/delegating", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }


    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }

    @Unroll
    def "Check non-tenanted AuthZ with #roles and expected response code #respcode"() {
        fakeIdentityService.with {
            client_token = "rackerButts"
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_userid = "456"
        }

        def reqHeaders =
                [
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token,
                        'x-roles': roles
                ]
        when: "User passes a request through repose with role #roles"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/serrrrrrrr", method: 'GET',
                headers: reqHeaders)

        then: "User with #roles should get response code #respcode"
        mc.receivedResponse.code == respcode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.findAll("x-delegated").contains(authDelegatingMsg)

        where: "User with #roles expect response code #respcode"
        roles               |respcode   | authDelegatingMsg
        'user-admin'        |"403"      | "q=.3"
        'non-admin'         |"403"      | "q=.3"
        null                |"403"      | "q=.3"
        ''                  |"403"      | "q=.3"
        'openstack%2Cadmin' |'403'      | "q=.3"
        'admin%20'          |'403'      | "q=.3"
    }

    def "When user requests a URL that is not in the user's service list should receive a 403 FORBIDDEN response"(){

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = token
        fakeIdentityService.originServicePort = 99999

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+token+"/ss", method:'GET', headers:['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        foundLogs.size() == 1
        mc.handlings.size() == 0
        mc.receivedResponse.code == "403"
    }
}

