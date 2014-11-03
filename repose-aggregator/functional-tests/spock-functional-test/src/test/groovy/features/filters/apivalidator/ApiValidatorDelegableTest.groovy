package features.filters.apivalidator
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
/**
 * Created by jennyvo on 11/3/14.
 */
class ApiValidatorDelegableTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable", params)
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
        When delegating is set to true, the invalid/fail request will be forwarded to
        - next filter (if exists) with failed message
        - to origin service with failed message and up to origin service handle
    */
    @Unroll("Delegable:headers=#headers, failed message=#delegateMsg")
    def "when delegating is true, Repose can delegate invalid request with failed reason to origin service handle"() {
        given:
        MessageChain mc

        when:
        mc = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)

        then:
        mc.getReceivedResponse().getCode().equals(responseCode)
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") == delegateMsg

        where:
        method  | path  | headers                                   | responseCode  | delegateMsg
        "GET"   | "/a"  | ["x-roles": "raxrole-test1"]              | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.5"
        "PUT"   | "/a"  | ["x-roles": "raxrole-test1, a:admin"]     | "200"         | "405;component=api-checker;msg=Bad method: PUT. The Method does not match the pattern: 'DELETE|GET|POST';q=0.5"
        "POST"  | "/a"  | ["x-roles": "raxrole-test1, a:observer"]  | "200"         | "405;component=api-checker;msg=Bad method: POST. The Method does not match the pattern: 'GET';q=0.5"
        "POST"  | "/a"  | ["x-roles": "raxrole-test1, a:bar"]       | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.5"
        "GET"   | "/b"  | ["x-roles": "raxrole-test2"]              | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.2"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2"]              | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.2"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2, b:observer"]  | "200"         | "404;component=api-checker;msg=Resource not found: /{a}. The URI segment does not match the pattern: 'b';q=0.2"
        "DELETE"| "/b"  | ["x-roles": "raxrole-test2, b:bar"]       | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.2"
        "POST"  | "/b"  | ["x-roles": "raxrole-test2, b:admin"]     | "200"         | "404;component=api-checker;msg=Resource not found: /{a}. The URI segment does not match the pattern: 'b';q=0.2"
        "PUT"   | "/b"  | ["x-roles": "raxrole-test2, a:admin"]     | "200"         | "404;component=api-checker;msg=Resource not found: /{a};q=0.2"
    }
}
