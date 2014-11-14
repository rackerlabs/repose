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
        given:
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
        def authDelegatingMsg = 'status_code=403.component=client-authorization.message=.*\\"http:\\/\\/\\w+([-|:\\d]+)\\/\\"\\.\\s+User not authorized to access service.;q=0.3'

        when: "User passes a request through repose with role #roles"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/servers/serrrrrrrr", method: 'GET',
                headers: reqHeaders)

        then: "User with #roles should get response code #respcode"
        mc.receivedResponse.code == respcode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.getFirstValue("x-delegated")=~ authDelegatingMsg

        where: "User with #roles expect response code #respcode"
        roles               |respcode
        'user-admin'        |"200"
        'non-admin'         |"200"
        null                |"200"
        ''                  |"200"
        'openstack%2Cadmin' |'200'
        'admin%20'          |'200'
    }

    def "When user requests a URL that is not in the user's service list repose should forward 403 FORBIDDEN to origin service"(){

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = token
        fakeIdentityService.originServicePort = 99999
        def strregex = 'status_code=403.component=client-authorization.message=.*\\"http:\\/\\/\\w+([-|:\\d]+)\\/\\"\\.\\s+User not authorized to access service.;q=0.3'

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url:reposeEndpoint + "/v1/"+token+"/ss", method:'GET', headers:['X-Auth-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in openstack-authorization.cfg.xml")

        then: "Repose should forward to origin service with failure message"
        foundLogs.size() == 1
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains("x-delegated")
        mc.handlings[0].request.headers.getFirstValue("x-delegated") =~ strregex
    }
}

