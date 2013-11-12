package features.filters.clientauthn.tenantvalidation
import features.filters.clientauthn.IdentityServiceRemoveTenantedValidationResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
import org.rackspace.gdeproxy.Response
import spock.lang.Unroll

class TenantedNonDelegableTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static IdentityServiceRemoveTenantedValidationResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/removetenant",
                "features/filters/clientauthn/removetenant/tenantednondelegable")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new IdentityServiceRemoveTenantedValidationResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)


    }

    def cleanupSpec() {
        deproxy.shutdown()

        repose.stop()
    }

    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in tenanted and non delegable mode - fail"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = isAuthed
        fakeIdentityService.adminOk = isAdminAuthed

        when: "User passes a request through repose with tenant in service admin role = " + tenantWithAdminRole + " and tenant returned equal = " + tenantMatch
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        fakeIdentityService.isValidateClientTokenBroken = validateClientBroken
        fakeIdentityService.isGetAdminTokenBroken = getAdminTokenBroken
        fakeIdentityService.isGetGroupsBroken = getGroupsBroken
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        mc.receivedResponse.headers.contains("www-authenticate") == www_auth_header

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == 1
        mc.handlings.size() == 0

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | isAuthed | isAdminAuthed | responseCode | orphanedHandlings | www_auth_header | validateClientBroken | getAdminTokenBroken | getGroupsBroken
        111       | false       | false               | true     | false         | "500"        | 1                 | false           | false                | false               | false
        888       | true        | true                | true     | true          | "500"        | 1                 | false           | false                | true                | false
        555       | false       | false               | true     | true          | "401"        | 2                 | true            | false                | false               | false
        666       | false       | false               | false    | true          | "401"        | 1                 | true            | false                | false               | false
        777       | true        | true                | true     | true          | "500"        | 1                 | false           | true                 | false               | false
        100       | true        | true                | true     | true          | "500"        | 2                 | false           | false                | false               | true
    }

    @Unroll("Tenant: #reqTenant")
    def "when authenticating user in tenanted and non delegable mode - pass"() {

        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = tenantMatch
        fakeIdentityService.doesTenantHaveAdminRoles = tenantWithAdminRole
        fakeIdentityService.client_tenant = reqTenant
        fakeIdentityService.client_userid = reqTenant
        fakeIdentityService.isValidateClientTokenBroken = false
        fakeIdentityService.isGetAdminTokenBroken = false
        fakeIdentityService.isGetGroupsBroken = false
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-Default-Region") == "the-default-region"

        !mc.receivedResponse.headers.contains("www-authenticate")

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + "/servers/" + reqTenant + "/", 'GET', ['X-Auth-Token': fakeIdentityService.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.contains("X-Default-Region")
        mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"

        where:
        reqTenant | tenantMatch | tenantWithAdminRole | orphanedHandlings | cachedOrphanedHandlings
        222       | true        | true                | 2                 | 0
        333       | true        | false               | 2                 | 0
        444       | false       | true                | 2                 | 1
    }

    def "Should not split request headers according to rfc"() {
        given:
        def reqHeaders = ["user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36", "x-pp-user": "usertest1," +
                "usertest2, usertest3", "accept": "application/xml;q=1 , application/json;q=0.5"]
        Map<String, String> headers = ["X-Roles" : "group1", "Content-Type" : "application/xml"]
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = true
        fakeIdentityService.doesTenantHaveAdminRoles = true
        fakeIdentityService.client_tenant = 123
        fakeIdentityService.client_userid = 123
        fakeIdentityService.isValidateClientTokenBroken = false
        fakeIdentityService.isGetAdminTokenBroken = false
        fakeIdentityService.isGetGroupsBroken = false
        def respFromOrigin = deproxy.makeRequest(reposeEndpoint + "/servers/123/", 'GET', ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token] + reqHeaders)
        def sentRequest = ((MessageChain) respFromOrigin).getHandlings()[0]

        then:
        sentRequest.request.getHeaders().findAll("user-agent").size() == 1
        sentRequest.request.getHeaders().findAll("x-pp-user").size() == 4
        sentRequest.request.getHeaders().findAll("accept").size() == 2
    }

    def "Should not split response headers according to rfc"() {
        given: "Origin service returns headers "
        def respHeaders = ["location": "http://somehost.com/blah?a=b,c,d", "via": "application/xml;q=0.3, application/json;q=1"]
        def xmlResp = { request -> return new Response(201, "Created", respHeaders) }
        Map<String, String> headers = ["X-Roles" : "group1", "Content-Type" : "application/xml"]
        def clientToken = UUID.randomUUID().toString()
        fakeIdentityService.client_token = clientToken
        fakeIdentityService.tokenExpiresAt = (new DateTime()).plusDays(1);
        fakeIdentityService.ok = true
        fakeIdentityService.adminOk = true

        when: "User passes a request through repose"
        fakeIdentityService.isTenantMatch = true
        fakeIdentityService.doesTenantHaveAdminRoles = true
        fakeIdentityService.client_tenant = 123
        fakeIdentityService.client_userid = 123
        fakeIdentityService.isValidateClientTokenBroken = false
        fakeIdentityService.isGetAdminTokenBroken = false
        fakeIdentityService.isGetGroupsBroken = false
        def respFromOrigin =
            deproxy.makeRequest(url: reposeEndpoint + "/servers/123/", method: 'GET',
                    headers: ['content-type': 'application/json', 'X-Auth-Token': fakeIdentityService.client_token],
                    defaultHandler: xmlResp
            )

        then:
        respFromOrigin.receivedResponse.headers.findAll("location").size() == 1
        respFromOrigin.receivedResponse.headers.findAll("via").size() == 1
    }


}
