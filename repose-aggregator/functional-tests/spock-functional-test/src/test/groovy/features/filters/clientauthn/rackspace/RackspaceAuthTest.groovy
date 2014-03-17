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

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/rackspace", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new RackspaceIdentityServiceResponseSimulator()
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    @Unroll("when authenticating user with Rackspace identity - success - User: #user, Token: #token")
    def "when authenticating user with Rackspace identity - success"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = true
        fakeIdentityService.isAbleToGetGroups = true

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.orphanedHandlings.size() == 2
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.getFirstValue("X-auth-user") == user
        request2.headers.getFirstValue("host") == "localhost:${properties.targetPort}"
        request2.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        request2.headers.getFirstValue("x-auth-token") == token
        request2.headers.contains("x-pp-groups")
        request2.headers.getFirstValue("x-authorization") == "Proxy " + user

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == 0
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        mc.handlings[0].request.headers.getFirstValue("X-auth-user") == user
        mc.handlings[0].request.headers.getFirstValue("host") == "localhost:${properties.targetPort}"
        mc.handlings[0].request.headers.getFirstValue("x-forwarded-for") == "127.0.0.1"
        mc.handlings[0].request.headers.getFirstValue("x-auth-token") == token
        mc.handlings[0].request.headers.contains("x-pp-groups")
        mc.handlings[0].request.headers.getFirstValue("x-authorization") == "Proxy " + user

        where:
        user     | token    | contentType
        "rando5" | "toke4"  | "xml"
        "rando9" | "toke8"  | "json"
    }

    @Unroll("when authenticating user with Rackspace identity - failure - User: #user, Token: #token")
    def "when authenticating user with Rackspace identity - failure"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = isUserAuthed
        fakeIdentityService.isAbleToGetGroups = true

        when: "User passes a request through repose"
        MessageChain mc
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == orphanedHandlings

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == orphanedHandlings
        mc.handlings.size() == 0

        where:
                         user     | token    | isUserAuthed | responseCode | contentType | orphanedHandlings
/* empty user & token */ ""       | ""       | true         | "401"        | "xml"       | 0
/* empty token */        "rando4" | ""       | true         | "401"        | "xml"       | 0
/* empty user  */        ""       | "toke3"  | true         | "401"        | "xml"       | 0
/* fail belongsto */     "rando6" | "toke5"  | false        | "401"        | "json"      | 1
/* empty user & token */ ""       | ""       | true         | "401"        | "json"      | 0
/* empty token */        "rando8" | ""       | true         | "401"        | "json"      | 0
/* empty user  */        ""       | "toke7"  | true         | "401"        | "json"      | 0
    }

    @Category(Bug)
    def "rackspace auth should return 401 when unable to retrieve groups"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = true
        fakeIdentityService.isAbleToGetGroups = false

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.handlings.size() == 0
        mc.orphanedHandlings.size() == 1

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(
                url: "$reposeEndpoint/v1/$user",
                method: 'GET',
                headers: [
                        'content-type': "application/$contentType",
                        'X-Auth-User': user,
                        'X-Auth-Token': token
                ]
        )

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
