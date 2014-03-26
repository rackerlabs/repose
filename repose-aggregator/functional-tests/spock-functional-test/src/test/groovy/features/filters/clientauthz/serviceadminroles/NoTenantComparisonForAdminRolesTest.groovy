package features.filters.clientauthz.serviceadminroles

import framework.ReposeConfigurationProvider
import framework.category.Bug
import org.junit.experimental.categories.Category

//import framework.ReposeInProcessValveLauncher
import framework.mocks.MockIdentityService
import framework.ReposeValveTest
import groovy.json.JsonBuilder
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

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
        def configurationProvider = repose.configurationProvider
        configurationProvider.applyConfigs("common", params)
        configurationProvider.applyConfigs("features/filters/clientauthz/serviceadminroles", params)
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

    /**
     * These tests all pass.  They shouldn't!
     * @return
     */
    @Category(Bug.class)
    @Unroll("tenant: #requestTenant with request role #requestRole, with return from identity with response tenant: #responseTenant and role: #serviceAdminRole")
    def "When accessing tenant's resource - fail"() {

        given:
        def headers = [
                'X-Auth-Token': UUID.randomUUID().toString()
        ]

        identityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        if(authResponseCode != 200){
            identityService.validateTokenHandler = {
                tokenId, request,xml ->
                    new Response(authResponseCode)
            }
        }
        when:
        def mc = deproxy.makeRequest(url: "${reposeEndpoint}/$requestTenant/resource", headers: headers + requestRole)

        then:
        mc.receivedResponse.code == "403"
        mc.handlings.size() == 1

        where:
        requestTenant | responseTenant | serviceAdminRole  | requestRole                 | authResponseCode
        124           | 456            | "non-admin"       | []                          | 200
        125           | 456            | "non-admin"       | ['X-roles':"some-role"]     | 200
        128           | null           | "non-admin"       | ['X-roles':"some-role"]     | 200
        123           | 123            | "non-admin"       | ['X-roles':"some-role"]     | 404
        126           | 456            | "service-admin"   | ['X-roles':"some-role"]     | 404
        129           | 456            | "some-role"       | ['X-roles':"service-admin"] | 404
    }

    @Unroll("tenant: #requestTenant with request role #requestRole, with return from identity with response tenant: #responseTenant and role: #serviceAdminRole")
    def "When accessing tenant's resource - pass"() {

        given:
        def headers = [
                'X-Auth-Token': UUID.randomUUID().toString()
        ]

        identityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = responseTenant
            client_userid = requestTenant
            service_admin_role = serviceAdminRole
        }

        when:
        def mc = deproxy.makeRequest(url: "${reposeEndpoint}/$requestTenant/resource", headers: headers + requestRole)

        then:
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1

        where:
        requestTenant | responseTenant | serviceAdminRole  | requestRole                 | responseCode
        223           | 223            | "non-admin"       | ['X-roles':"some-role"]     | "200"
        225           | 456            | "service-admin"   | ['X-roles':"some-role"]     | "200"
        226           | 456            | "some-role"       | ['X-roles':"some-role"]     | "200"
        229           | 456            | "some-role"       | ['X-roles':"service-admin"] | "200"
        227           | null           | "service-admin"   | ['X-roles':"some-role"]     | "200"
        224           | null           | "some-role"       | ['X-roles':"service-admin"] | "200"
        228           | 456            | "some-role"       | ['X-roles':"service-admin"] | "200"
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
