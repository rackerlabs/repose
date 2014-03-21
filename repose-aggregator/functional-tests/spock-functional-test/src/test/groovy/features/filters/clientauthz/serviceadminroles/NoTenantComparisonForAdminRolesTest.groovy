package features.filters.clientauthz.serviceadminroles

import framework.ReposeConfigurationProvider
//import framework.ReposeInProcessValveLauncher
import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import groovy.json.JsonBuilder
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import spock.lang.Ignore

class NoTenantComparisonForAdminRolesTest extends ReposeValveTest {

    static MockIdentityService identityService

    def setupSpec() {

        identityService = new MockIdentityService(properties.identityPort, properties.targetPort)

        deproxy = new Deproxy()
        def handler = { Request request ->
            println("${request.method} ${request.path}")
            return identityService.handler(request)
        }
        deproxy.addEndpoint(name: "identityService", port: properties.identityPort, defaultHandler: handler)
        deproxy.addEndpoint(name: "originService", port: properties.targetPort)

        def params = properties.defaultTemplateParams
//        def configurationProvider = new ReposeConfigurationProvider(properties)
        def configurationProvider = repose.configurationProvider
        configurationProvider.applyConfigs("common", params)
        configurationProvider.applyConfigs("features/filters/clientauthz/serviceadminroles", params)

//        repose = new ReposeInProcessValveLauncher(configurationProvider, properties.configDirectory, properties.reposeShutdownPort)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def setup() {
        identityService.resetCounts()
        identityService.resetHandlers()
    }

    static int userCount = 0;
    String getNewUniqueUser() {

        String name = "user-${userCount}"
        userCount++;
        return name;
    }

    def "Admin user with no tenant tries to access a tenant's resource"() {

        given:
        def username = getNewUniqueUser()
        def headers = [
                'X-Auth-Token': UUID.randomUUID().toString(),
        ]

        identityService.client_tenant = 'tenant456'
//        identityService.defaultUserRoles = [
//                [
//                        id: '2468',
//                        name: 'service-admin',
//                        description: 'The admin for the service',
//                        serviceId: '0000000000000000000000000000000000000001',
//                ]
//        ]

        when:
        def mc = deproxy.makeRequest(url: "${reposeEndpoint}/tenant123/resource", headers: headers)

        then:
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }



    def cleanupSpec() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }

}
