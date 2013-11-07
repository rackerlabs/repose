package features.filters.clientauthn.rackspace

import features.filters.clientauthn.RackspaceIdentityServiceResponseSimulator
import framework.ReposeValveTest
import framework.category.Bug
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class RackspaceAuthTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static RackspaceIdentityServiceResponseSimulator fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        repose.applyConfigs("features/filters/clientauthn/rackspace")
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.getProperty("target.port").toInteger(), 'origin service')
        fakeIdentityService = new RackspaceIdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.getProperty("identity.port").toInteger(),
                'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("User: #user, Token: #token")
    def "when authenticating user with Rackspace identity - success"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = true
        fakeIdentityService.isAbleToGetGroups = true

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 2
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-auth-user") == user
        request2.headers.getFirstValue("host") == "localhost:10001"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-auth-token") == token
        request2.headers.contains("x-pp-groups")
        request2.headers.getFirstValue("x-authorization") == "Proxy " + user

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.getFirstValue("X-auth-user") == user
        mc.handlings[0].request.headers.getFirstValue("host") == "localhost:10001"
        mc.handlings[0].request.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        mc.handlings[0].request.headers.getFirstValue("x-auth-token") == token
        mc.handlings[0].request.headers.contains("x-pp-groups")
        mc.handlings[0].request.headers.getFirstValue("x-authorization") == "Proxy " + user

        where:
        user     | token    | contentType
        "rando5" | "toke4"  | "xml"
        "rando9" | "toke8"  | "json"
    }

    @Unroll("User: #user, Token: #token")
    def "when authenticating user with Rackspace identity - failure"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = isUserAuthed
        fakeIdentityService.isAbleToGetGroups = true

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == 1
        mc.handlings.size() == 0

        where:
                         user     | token    | isUserAuthed | responseCode | contentType
/* fail belongsto */     "rando2" | "toke1"  | false        | "401"        | "xml"
/* empty user & token */ null     | null     | true         | "401"        | "xml"
/* empty token */        "rando4" | null     | true         | "401"        | "xml"
/* empty user  */        null     | "toke3"  | true         | "401"        | "xml"
/* fail belongsto */     "rando6" | "toke5"  | false        | "401"        | "json"
/* empty user & token */ null     | null     | true         | "401"        | "json"
/* empty token */        "rando8" | null     | true         | "401"        | "json"
/* empty user  */        null     | "toke7"  | true         | "401"        | "json"
    }

    @Category(Bug)
    def "rackspace auth should return 401 when unable to retrieve groups"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = true
        fakeIdentityService.isAbleToGetGroups = false

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(url: reposeEndpoint + "/v1/" + user, method: 'GET', headers: ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1

        where:
        user     | token   | contentType
        "rando3" | "toke2" | "xml"
        "rando7" | "toke6" | "json"
    }
}
