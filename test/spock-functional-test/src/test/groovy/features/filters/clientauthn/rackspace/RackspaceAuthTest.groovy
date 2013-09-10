package features.filters.clientauthn.rackspace

import features.filters.clientauthn.RackspaceIdentityServiceResponseSimulator
import framework.ReposeValveTest
import org.joda.time.DateTime
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain
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
    def "when authenticating user with Rackspace identity"() {

        fakeIdentityService.client_token = token
        fakeIdentityService.isTokenAuthenticated = isUserAuthed
        fakeIdentityService.isAbleToGetGroups = groupsRetrieved

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(reposeEndpoint + request + user, 'GET', ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == handlings
        mc.orphanedHandlings.size() == orphanedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            def request2 = mc.handlings[0].request
            request2.headers.contains("X-Default-Region")
            request2.headers.getFirstValue("X-Default-Region") == "the-default-region"
            request2.headers.contains("x-auth-token")
            request2.headers.contains("x-identity-status")
            request2.headers.contains("x-authorization")
            request2.headers.getFirstValue("authorization") == "Authorization: Basic YWRtaW5fdXNlcm5hbWU6YWRtaW5fcGFzc3dvcmQ="
            request2.headers.getFirstValue("x-identity-status") == "Confirmed"
            request2.headers.getFirstValue("x-authorization") == "Proxy"
        }

        when: "User passes a request through repose the second time"
        mc = deproxy.makeRequest(reposeEndpoint + request + user, 'GET', ['content-type': 'application/' + contentType, 'X-Auth-User': user, 'X-Auth-Token': token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.orphanedHandlings.size() == cachedOrphanedHandlings
        mc.handlings.size() == cachedHandlings
        if (mc.handlings.size() > 0) {
            mc.handlings[0].endpoint == originEndpoint
            mc.handlings[0].request.headers.contains("X-Default-Region")
            mc.handlings[0].request.headers.getFirstValue("X-Default-Region") == "the-default-region"
        }

        where:
         request                 | user     | token    | isUserAuthed | responseCode | handlings | orphanedHandlings | cachedOrphanedHandlings | cachedHandlings | contentType | groupsRetrieved
/* fail belongsto */     "/v1/"  | "rando2" | "toke1"  | false        | "401"        | 0         | 1                 | 1                       | 0               | "xml"       | true
/* fail groups --this should be a 401!!!    */     "/v1/"  | "rando3" | "toke2"  | true         | "200"        | 1         | 2                 | 1                       | 1               | "xml"       | false
/* empty user & token */ "/v1/"  | null     | null     | true         | "401"        | 0         | 1                 | 1                       | 0               | "xml"       | true
/* empty token */        "/v1/"  | "rando4" | null     | true         | "401"        | 0         | 1                 | 1                       | 0               | "xml"       | true
/* empty user  */        "/v1/"  | null     | "toke3"  | true         | "401"        | 0         | 1                 | 1                       | 0               | "xml"       | true
/* success     */        "/v1/"  | "rando5" | "toke4"  | true         | "200"        | 1         | 2                 | 0                       | 1               | "xml"       | true
/* fail belongsto */     "/v1/"  | "rando6" | "toke5"  | false        | "401"        | 0         | 1                 | 1                       | 0               | "json"      | true
/* fail groups -- this should be a 401!!! */        "/v1/"  | "rando7" | "toke6"  | true         | "200"        | 1         | 2                 | 1                       | 1               | "json"      | false
/* empty user & token */ "/v1/"  | null     | null     | true         | "401"        | 0         | 1                 | 1                       | 0               | "json"      | true
/* empty token */        "/v1/"  | "rando8" | null     | true         | "401"        | 0         | 1                 | 1                       | 0               | "json"      | true
/* empty user  */        "/v1/"  | null     | "toke7"  | true         | "401"        | 0         | 1                 | 1                       | 0               | "json"      | true
/* success     */        "/v1/"  | "rando9" | "toke8"  | true         | "200"        | 1         | 2                 | 0                       | 1               | "json"      | true
    }


}
