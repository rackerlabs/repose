package features.filters.clientauthz.serviceresponse

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 4/4/14.
 */
class NonTenantedAuthZTest extends ReposeValveTest{
    def static originEndpoint
    def static identityEndpoint

    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthz/nontenanted", params)
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

    @Unroll("Check non-tenanted AuthZ with #roles and expected response code #respcode")
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

        where: "User with #roles expect response code #respcode"
        roles                   |respcode
        'user-admin'            |"403"
        'non-admin'             |"403"
        'admin'                 |"200"
        'openstack:admin'       |"200"
    }
}
