package features.filters.clientauthn.multitenantswquality

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 1/13/15.
 */
class MultiTenantswQualityTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def static params

    def setupSpec() {

        deproxy = new Deproxy()

        params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/multitenantswquality", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    def setup(){
        fakeIdentityService.resetHandlers()
    }

    @Unroll("#defaultTenant, #secondTenant, #requestTenant ")
    def "When user token have multi-tenant will retrieve all tenants in the header" () {
        given:
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_tenant = defaultTenant
            client_tenant_file = secondTenant
            service_admin_role = "not-admin"
        }

        when: "User passes a request through repose with $requestTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").size() == numberTenants
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").contains(defaultTenant+";q=1.0")
            if (!secondTenant.equals(defaultTenant)) {
                assert mc.handlings[0].request.headers.findAll("x-tenant-id").contains(secondTenant+";q=0.5")
            }
        }

        where:
        defaultTenant   | secondTenant  |requestTenant  |clientToken        |serviceRespCode    | numberTenants
        "123456"        | "nast-id"     | "123456"      |UUID.randomUUID()  | "200"             | 2
        "123456"        | "nast-id"     | "nast-id"     |UUID.randomUUID()  | "200"             | 2
        "123456"        | "123456"      | "123456"      |UUID.randomUUID()  | "200"             | 1
        "123456"        | "nast-id"     | "223456"      |UUID.randomUUID()  | "401"             | 0
    }


    def "With legacy xsd namespace: when user token have multi-tenant will retrieve all tenants in the header" () {
        given:
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/multitenantswquality/oldnamespace", params, /*sleepTime*/ 10)
        fakeIdentityService.with {
            client_token = clientToken
            tokenExpiresAt = (new DateTime()).plusDays(1)
            client_tenant = defaultTenant
            client_tenant_file = secondTenant
            service_admin_role = "not-admin"
        }

        when: "User passes a request through repose with $requestTenant"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$requestTenant",
                method: 'GET',
                headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Everything gets passed as is to the origin service (no matter the user)"
        mc.receivedResponse.code == serviceRespCode

        if (serviceRespCode != "200")
            assert mc.handlings.size() == 0
        else {
            assert mc.handlings.size() == 1
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").size() == numberTenants
            assert mc.handlings[0].request.headers.findAll("x-tenant-id").contains(defaultTenant+";q=1.0")
            if (!secondTenant.equals(defaultTenant)) {
                assert mc.handlings[0].request.headers.findAll("x-tenant-id").contains(secondTenant+";q=0.5")
            }
        }

        where:
        defaultTenant   | secondTenant  |requestTenant  |clientToken        |serviceRespCode    | numberTenants
        "123456"        | "nast-id"     | "123456"      |UUID.randomUUID()  | "200"             | 2
    }


}

