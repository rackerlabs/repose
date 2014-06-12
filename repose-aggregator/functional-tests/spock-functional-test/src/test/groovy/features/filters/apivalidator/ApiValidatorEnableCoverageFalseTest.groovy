package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
import org.junit.experimental.categories.Category

/**
 * Created by jennyvo on 6/12/14.
 */
@Category(Slow.class)
class ApiValidatorEnableCoverageFalseTest extends ReposeValveTest{
    String intrumentedHandler = '\"com.rackspace.com.papi.components.checker.handler\":*'
    def static s0_count = 0
    def S0 = 0
    def SA = 0
    def S0_a_admin = 0
    String s0str = 'name=\"S0\"'
    String sastr = 'name=\"SA\"'
    String s0adminstr = 'name=\"S0_a_admin\"'

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/statemachine/enableapicoveragefalse", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }
    /*
        When enable-api-coverage is set to false, enable-rax-role is set to true,
        certain user roles will allow to access certain methods according to config in the wadl.
        i.e. 'GET' method only be available to access by a:observer and a:admin role
    */
    @Unroll("enableapicoverage false:headers=#headers")
    def "when enable-api-coverage is false, validate count at state level"() {
        given:
        File outputfile = new File("output.dot")
        if (outputfile.exists())
            outputfile.delete()
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/a", method: method, headers: headers)
        if(headers != null)
            s0_count = s0_count + 1

        def getBeanObj = repose.jmx.getMBeanNames(intrumentedHandler)

        def validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
        def checkstrscope = 'scope=\"raxRolesEnabled_'
        def check = false

        def getMBeanObj = repose.jmx.getMBeanNames(validatorBeanDomain)

        def strIt = getMBeanObj[0].toString()
        println(strIt)
        if(strIt.contains(checkstrscope))
            check = true

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)
        getBeanObj.size() == 0      // not using handler
        check == true

        where:
        method   | headers                                             | responseCode
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"
        "GET"    | ["x-roles": "raxRolesEnabled"]                      | "404"
        "GET"    | ["x-roles": "raxRolesEnabled, a:creator"]           | "404"
        "GET"    | null                                                | "403"
    }
}
