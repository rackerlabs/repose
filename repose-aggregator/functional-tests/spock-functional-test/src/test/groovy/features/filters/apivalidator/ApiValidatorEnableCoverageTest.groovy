package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import javax.management.ObjectName

/**
 * Created by jennyvo on 6/11/14.
 *  This test verify api stage machine coverage
 */

class ApiValidatorEnableCoverageTest extends ReposeValveTest{
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
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/statemachine", params)
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
        When enable-api-coverage is set to true, enable-rax-role is set to true,
        certain user roles will allow to access certain methods according to config in the wadl.
        i.e. 'GET' method only be available to access by a:observer and a:admin role
    */
    @Unroll("enableapicoverage:headers=#headers,expected S0_a_admin:#S0_a_admin_count, SA:#SA_count")
    def "when enable-api-coverage is true, validate count at state level"() {
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

        getBeanObj.each {
            //println it.toString()
            def strIt = it.toString()
            if (strIt.contains(s0str)){
                S0 = repose.jmx.getMBeanAttribute(it.toString(), "Count")
            }

            if (strIt.contains(sastr))
                SA = repose.jmx.getMBeanAttribute(it.toString(), "Count")

            if (it.toString().contains(s0adminstr))
                S0_a_admin = repose.jmx.getMBeanAttribute(it.toString(), "Count")
        }

        then:
        messageChain.getReceivedResponse().getCode().equals(responseCode)
        S0 == s0_count
        SA == SA_count
        S0_a_admin == S0_a_admin_count

        where:
        method   | headers                                             | responseCode   | SA_count  | S0_a_admin_count
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer"]          | "200"          | 1         | 0
        "GET"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "200"          | 2         | 0
        "GET"    | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"          | 3         | 1
        "GET"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"          | 4         | 2
        "GET"    | ["x-roles": "raxRolesEnabled"]                      | "404"          | 4         | 2
        "GET"    | ["x-roles": "raxRolesEnabled, a:creator"]           | "404"          | 4         | 2
        "GET"    | null                                                | "403"          | 4         | 2
        "POST"   | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"          | 5         | 3
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"          | 6         | 4
        "POST"   | ["x-roles": "raxRolesEnabled"]                      | "404"          | 6         | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:observer"]          | "405"          | 6         | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"          | 6         | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:bar, a:observer"]   | "405"          | 6         | 4
        "POST"   | ["x-roles": "raxRolesEnabled, a:creator"]           | "404"          | 6         | 4
        "POST"   | null                                                | "403"          | 6         | 4//this will not effect config change
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin"]             | "200"          | 7         | 5
        "DELETE" | ["x-roles": "raxRolesEnabled, a:admin, a:bar"]      | "200"          | 8         | 6
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:admin"]      | "200"          | 9         | 7
        "DELETE" | ["x-roles": "raxRolesEnabled, a:observer, a:admin"] | "200"          | 10        | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"          | 10        | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "404"          | 10        | 8
        "DELETE" | ["x-roles": "raxRolesEnabled, observer, creator"]   | "404"          | 10        | 8
        "DELETE" | null                                                | "403"          | 10        | 8//this will not effect config change
        // PUT method is not available in wadl should expect to get 405 to whoever rax-role
        "PUT"    | ["x-roles": "raxRolesEnabled"]                      | "404"          | 10        | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar"]               | "404"          | 10        | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:observer, a:bar"]   | "405"          | 10        | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:bar, a:jawsome"]    | "404"          | 10        | 8
        "PUT"    | ["x-roles": "raxRolesEnabled, a:admin"]             | "405"          | 10        | 9
    }

}
