package features.filters.clientauthn.delegable

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/10/14.
 */
class ClientAuthNAndApiValidatorDelegableTest  extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityService fakeIdentityService

    def setupSpec() {

        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/delegable", params)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/delegable/withapivalidator", params)
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
    /*
        This test to verify the forward fail reason and default quality for authn
    */
    @Unroll("req method: #method, #path, #apiDelegatedMsg")
    def "when req without token, non tenanted and delegable mode with quality"() {
        given:
        fakeIdentityService.with {
            client_token = ""
            tokenExpiresAt = (new DateTime()).plusDays(1);
            service_admin_role = "non-admin"
        }
        Map<String, String> headers = ["X-Roles" : roles,
                                       "Content-Type" : "application/xml",
                                       "X-Auth-Token": fakeIdentityService.client_token]
        def authDelegatedMsg = 'status_code=401.component=client-auth-n.message=Failure in AuthN filter.;q=0.3'

        when: "User passes a request through repose with authN and apiValidator delegable"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/$path",
                method: method,
                headers: headers)

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == originEndpoint
        def request2 = mc.handlings[0].request
        request2.headers.contains("x-identity-status")
        request2.headers.contains("x-authorization")
        request2.headers.getFirstValue("x-identity-status") == identityStatus
        request2.headers.getFirstValue("x-authorization") == "Proxy"
        request2.headers.findAll("x-delegated").size() == 2
        msgCheckingHelper(request2.headers.findAll("x-delegated"),authDelegatedMsg, apiDelegatedMsg)

        where:
        method  | path           | roles                 | identityStatus  |apiDelegatedMsg
        "GET"   | "servers/"     |"raxRole"              | "Indeterminate" | "status_code=403.component=api-checker.message=You are forbidden to perform the operation;q=0.6"
        "POST"  | "servers/1235" |"raxRole, a:observer"  | "Indeterminate" | "status_code=404.component=api-checker.message=Resource not found:\\s/servers/.*;q=0.6"
        "PUT"   | "servers/"     |"raxRole, a:admin"     | "Indeterminate" | "status_code=404.component=api-checker.message=Bad method: PUT. The Method does not match the pattern: 'DELETE|GET|POST';q=0.6"
        "DELETE"| "servers/test" |"raxRole, a:observer"  | "Indeterminate" | "status_code=404.component=api-checker.message=Resource not found:\\s/servers/.*;q=0.6"
        "GET"   | "get/"         |"raxRole"              | "Indeterminate" | "status_code=404.component=api-checker.message=Resource not found:\\s/.*;q=0.6"

    }

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
