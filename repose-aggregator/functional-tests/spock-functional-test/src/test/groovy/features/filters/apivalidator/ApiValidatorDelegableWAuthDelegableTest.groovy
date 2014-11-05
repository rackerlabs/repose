package features.filters.apivalidator
import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll
/**
 * Created by jennyvo on 11/4/14.
 *  Multiple filters with delegable option:
 *  - Api Validator with delegable set to true
 *  - Client Auth with delegating set to true
 */
class ApiValidatorDelegableWAuthDelegableTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable/withauthdelegable", params)
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
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    @Unroll("Request with tenant:#reqtenant,admin-role: #adminrole, roles: #roles, method: #method")
    def "when using api validate with delegate and auth client"() {

        given:
        fakeIdentityService.with {
            client_tenant = 12345
            client_token = clienttoken
            tokenExpiresAt = DateTime.now().plusDays(1)
            service_admin_role = adminrole
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/a/$reqtenant",
                method: method,
                headers: [
                        'x-roles': roles,
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        and: "If request made it to origin service"
        if (identitystatus == "Confirmed") {
            def request2 = mc.handlings[0].request
            assert(mc.handlings[0].endpoint == originEndpoint)
            assert(request2.headers.contains("x-auth-token"))
            assert(request2.headers.contains("x-identity-status"))
            assert(request2.headers.contains("x-authorization"))
            assert(request2.headers.getFirstValue("x-identity-status") == identitystatus)
            assert(request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        and: "If identityStatus was Indeterminate"
        if (identitystatus == "Indeterminate") {

            def request2 = mc.handlings[0].request
            assert(request2.headers.getFirstValue("x-identity-status") == identitystatus)
            assert(request2.headers.getFirstValue("x-authorization").startsWith("Proxy"))
        }

        where:
        reqtenant| adminrole | clienttoken     | roles                      | method   | responseCode | identitystatus | delegateMsg
        ""       | "regular" |""               | "raxrole-test1"            | "GET"    | "200"        | "Indeterminate"| "404;component=api-checker;msg=Resource not found: /{a};q=0.5"
        "test"   | "admin1"  |UUID.randomUUID()| "raxrole-test1,a:observer" | "POST"   | "200"        | "Confirmed"    | "404;component=api-checker;msg=Resource not found: /a/{test};q=0.5"
        "test"   | "admin2"  |UUID.randomUUID()| "raxrole-test1,a:observer" | "DELETE" | "200"        | "Confirmed"    | "404;component=api-checker;msg=Resource not found: /a/{test};q=0.5"
        12345    | "default" |UUID.randomUUID()| "raxrole-test1,a:admin"    | "PUT"    | "200"        | "confirmed"    | "404;component=api-checker;msg=Resource not found: /a/{12345};q=0.5"
    }

    @Unroll("Sending request with roles: #roles and admin resp: #authresp")
    def "when failing to authenticate admin client"() {

        given:
        fakeIdentityService.with {
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
            client_tenant = 123
        }

        if(authresp != 200){
            fakeIdentityService.validateTokenHandler = {
                tokenId, request,xml ->
                    new Response(authresp)
            }
        }
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/a/123",
                method: method,
                headers: [
                        'x-roles': roles,
                        'content-type': 'application/json',
                        'X-Auth-Token': fakeIdentityService.client_token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0

        where:
        roles                       | method     | authresp     | responseCode
        "raxrole-test1"             | "GET"      | 401          |"500"
        "raxrole-test1,a:observer"  | "POST"     | 404          |"401"
    }
}