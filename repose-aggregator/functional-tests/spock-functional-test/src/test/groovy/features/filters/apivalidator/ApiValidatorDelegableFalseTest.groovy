package features.filters.apivalidator

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/3/14.
 */
class ApiValidatorDelegableFalseTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable/delegablefalse", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def static params
    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }
    /*
        When delegating is set to false, the invalid/fail request will not be forwarded
    */
    @Unroll("Delegable false:headers=#headers")
    def "when delegating is false, Repose can delegate invalid request with failed reason to origin service handle" () {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)
        mc.handlings.size() == 0

        where:
        method  | path  | headers                                   | responseCode
        "GET"   | "/a"  | ["x-roles": "raxrole-test1"]              | "404"
        "PUT"   | "/a"  | ["x-roles": "raxrole-test1, a:admin"]     | "405"
        "POST"  | "/a"  | ["x-roles": "raxrole-test1, a:observer"]  | "405"
        "POST"  | "/a"  | ["x-roles": "raxrole-test1, a:bar"]       | "404"
    }

    /*
    When delegating is set to true, the invalid/fail request will be forwarded to
    - next filter (if exists) with failed message
    - to origin service with failed message and up to origin service handle
*/
    @Unroll("Delegable:headers=#headers, failed message=#delegateMsg")
    def "when delegating is true, Repose can delegate invalid request with failed reason to origin service handle"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        method  | path  | headers                                   | responseCode  | delegateMsg
        "GET"   | "/b"  | ["x-roles": "raxrole-test2"]              | "200"         | "404;component=api-checker;msg=Resource not found: /{b};q=0.5"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2"]              | "200"         | "404;component=api-checker;msg=Resource not found: /{b};q=0.5"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2, b:observer"]  | "200"         | "405;component=api-checker;msg=Bad method: PUT. The Method does not match the pattern: 'GET';q=0.5"
        "DELETE"| "/b"  | ["x-roles": "raxrole-test2, b:bar"]       | "200"         | "404;component=api-checker;msg=Resource not found: /{b};q=0.5"
        "POST"  | "/b"  | ["x-roles": "raxrole-test2, b:admin"]     | "200"         | "405;component=api-checker;msg=Bad method: POST. The Method does not match the pattern: 'DELETE|GET|PUT';q=0.5"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2, a:admin"]     | "200"         | "404;component=api-checker;msg=Resource not found: /{b};q=0.5"
    }
}
