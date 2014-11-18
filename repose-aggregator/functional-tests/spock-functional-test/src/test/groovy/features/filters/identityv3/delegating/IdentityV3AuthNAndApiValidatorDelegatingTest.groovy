package features.filters.identityv3.delegating

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/18/14.
 * Multi filters identity v3 authn and Api validator with delegating mode
 */
class IdentityV3AuthNAndApiValidatorDelegatingTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/delegating", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/delegating/apivalidator", params)
        repose.start()
        waitUntilReadyToServiceRequests('200')
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        sleep(500)
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll ("When #method req without credential with #roles to #path")
    def "when send req without credential with delegating option repose forward req and failure msg to origin service"() {
        given:
        def delegatingmsg = "status_code=401.component=openstack-identity-v3.message=A subject token was not provided to validate;q=0.7"
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456",
                method: method,
                headers: ['content-type': 'application/json', 'x-roles': roles])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization")
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
        mc.handlings[0].request.headers.contains("X-Delegated") 
        msgCheckingHelper(mc.handlings[0].request.headers.findAll("X-Delegated"),delegatingmsg,apiDelegatingMsg)

        where:
        method  | path          |roles                       | apiDelegatingMsg
        "GET"   |"/servers/"    |"raxrole-test1"             | "status_code=404.component=api-checker.message=.*;q=0.3"
        "POST"  |"/servers/1234"|"raxrole-test1, a:observer" | "status_code=404.component=api-checker.message=.*;q=0.3"
        "PUT"   |"/servers/"    |"raxrole-test1, a:admin"    | "status_code=404.component=api-checker.message=.*;q=0.3"
        "DELETE"|"/servers/"    |"raxrole-test1"             | "status_code=404.component=api-checker.message=.*;q=0.3"
        "GET"   |"/servers/"    |null                        | "status_code=404.component=api-checker.message=.*;q=0.3"
        "GET"   |"/get"         |"raxrole-test1, a:observer" | "status_code=404.component=api-checker.message=.*;q=0.3"
    }

    @Unroll("#authResponseCode, #responseCode, #roles")
    def "when send req with unauthorized user with forward-unauthorized-request true"() {
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_projectid = reqProject
            service_admin_role = "not-admin"
        }

        if(authResponseCode != 200){
            fakeIdentityV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode, null, null, responseBody)
            }
        }
        def apidelegatingmsg = "status_code=403.component=api-checker.message=.*;q=0.3"
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject",
                method: 'GET',
                headers: ['content-type': 'application/json',
                          'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization") == "Proxy"
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
        mc.handlings[0].request.headers.contains("X-Delegated")
        msgCheckingHelper(mc.handlings[0].request.headers.findAll("X-Delegated"),delegatingMsg,apidelegatingmsg)

        where:
        reqProject  | authResponseCode | responseCode   |responseBody                                           | delegatingMsg
        "p500"      | 401              | "200"          |"Unauthorized"                                         | "status_code=500.component=openstack-identity-v3.message=Valid admin token could not be fetched;q=0.7"
        "p501"      | 403              | "200"          |"Unauthorized"                                         | "status_code=500.component=openstack-identity-v3.message=Failed to validate subject token;q=0.7"
        "p502"      | 404              | "200"          |fakeIdentityV3Service.identityFailureJsonRespTemplate  | "status_code=401.component=openstack-identity-v3.message=Failed to validate subject token;q=0.7"
    }

    //helper function to validate delegating auth and api-checker messages
    def void msgCheckingHelper(List delegatingmsgs, String authmsg, String apimsg) {
        for (int i=0; i <delegatingmsgs.size(); i++) {
            if (delegatingmsgs.get(i).toString().contains("api-checker")){
                assert delegatingmsgs.get(i) =~ apimsg
            } else {
                assert delegatingmsgs.get(i) =~ authmsg
            }
        }
    }
}
