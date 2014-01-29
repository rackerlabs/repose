package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * A test to verify that a user can validate roles via api-checker
 * by setting the enable-rax-roles attribute on a validator.
 */
class ClientRaxRolesTest extends ReposeValveTest {

    def setup() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanup() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll("client_a:method=#method,headers=#headers,request=#request expected response=#responseCode")
    def "when enable-rax-roles is true, validate with wadl method level roles"() {
        given:
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/clientraxroles/client_a", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + request, method: method, headers: headers)

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)

        where:
        method | request                 | headers                                                       | responseCode
        "GET"  | "/v1/application.wadl"  | ["x-roles": "default,client_a:admin"]                           | "200"
        /*"GET"    | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "403"
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "403"
        "GET"    | null                                                | "403"
        "PUT"    | ["x-roles": "raxRolesEnabled"]                      | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar"]               | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "200"
        "PUT"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "POST"   | ["x-roles": "raxRolesEnabled"]                      | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:observer"]          | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar"]               | "403"
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:observer"]   | "403"
        "POST"   | null                                                | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled"]                      | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin, a:bar"]      | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:admin"] | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar"]               | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "403"
        "DELETE" | ["x-roles": "raxRolesEnabled, observer, creator"]   | "403"
        "DELETE" | null                                                | "403"  */
    }

}
