package features.filters.apivalidator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy

class ApiValidatorJMXTest extends ReposeValveTest {
    String validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    String validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    String PREFIX = "\"repose-node-com.rackspace.papi.filters\":type=\"ApiValidator\",scope=\""

    String NAME_ROLE_1 = "\",name=\"role-1\""
    String NAME_ROLE_2 = "\",name=\"role-2\""
    String NAME_ROLE_3 = "\",name=\"role-3\""
    String NAME_ROLE_ALL = "\",name=\"ACROSS ALL\""

    String API_VALIDATOR_1 = PREFIX + "api-validator" + NAME_ROLE_1
    String API_VALIDATOR_2 = PREFIX + "api-validator" + NAME_ROLE_2
    String API_VALIDATOR_3 = PREFIX + "api-validator" + NAME_ROLE_3
    String API_VALIDATOR_ALL = PREFIX + "api-validator" + NAME_ROLE_ALL


    def setup() {
        repose.applyConfigs(
                "features/filters/apivalidator/common",
                "features/filters/apivalidator/jmx")
        repose.start()
        sleep(15000)


        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        sleep(4000) //TODO: add a clean way to ensure deproxy has really shutdown all endpoints
        repose.stop()
    }

    def "when loading validators on startup, should register validator MXBeans"() {

        deproxy.makeRequest(reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        then:
        validatorBeans.size() == 3

    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        deproxy.makeRequest(reposeEndpoint + "/")

        given:
        def beforeUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        when: "I update the Repose API Validator filter with 2 new validators"
        repose.updateConfigs("features/filters/apivalidator/jmxupdate")

        and: "I send a request to Repose to ensure that the filter registers the new validator MBeans"
        def afterUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 2)

        then: "Repose has 2 validator MBeans, and they are not the same beans as before the update"
        afterUpdateBeans.size() == 2
        afterUpdateBeans.each { updatedBean ->
            beforeUpdateBeans.each {
                updatedBean.name != it.name
            }
        }
    }

    def "when request is for role-1, should increment invalid request for ApiValidator mbeans for role 1"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 1
    }

    def "when request is for role-2, should increment invalid request for ApiValidator mbeans for role 2"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 1
    }

    def "when request is for role-3, should increment invalid request for ApiValidator mbeans for role 3"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 1
    }

    def "when request is for role-3 and role-2, should increment invalid request for ApiValidator mbeans for role 3 and role 2"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 2
    }

    def "when request is for role-3 and role-1, should increment invalid request for ApiValidator mbeans for role 3 and role 1"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 2
    }

    def "when request is for role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 1 and role 2"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-1, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-1, role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == null
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 2
    }

    def "when request is for role-3, role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 3, role 1, and role 2"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-2, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-2, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 3
    }

    def "when request is for api validator, should increment ApiValidator mbeans for all"() {

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == 1
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == 3
    }

}