package features.filters.apivalidator
import framework.ReposeValveTest
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.junit.experimental.categories.Category

@Category(Slow.class)
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

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigsRuntime("common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigsRuntime("features/filters/apivalidator/jmx", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }


    def "when loading validators on startup, should register Configuration MXBeans"() {

        String ConfigurationBeanDomain = 'repose-node-com.rackspace.papi.jmx:*'
        String ConfigurationClassName = "com.rackspace.papi.jmx.ConfigurationInformation"

        deproxy.makeRequest(url:reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(ConfigurationBeanDomain, ConfigurationClassName, 1)

        then:
        validatorBeans.size() == 1

    }

    def "when loading validators on startup, should register validator MXBeans"() {

        deproxy.makeRequest(url:reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        then:
        validatorBeans.size() == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        deproxy.makeRequest(url:reposeEndpoint + "/")

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
                assert(updatedBean.name != it.name)
            }
        }
    }

    def "when request is for role-1, should increment invalid request for ApiValidator mbeans for role 1"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 1)
    }

    def "when request is for role-2, should increment invalid request for ApiValidator mbeans for role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 1)
    }

    def "when request is for role-3, should increment invalid request for ApiValidator mbeans for role 3"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 1)
    }

    def "when request is for role-3 and role-2, should increment invalid request for ApiValidator mbeans for role 3 and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 2)
    }

    def "when request is for role-3 and role-1, should increment invalid request for ApiValidator mbeans for role 3 and role 1"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 2)
    }

    def "when request is for role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 1 and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-1, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-1, role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 2)
    }

    def "when request is for role-3, role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 3, role 1, and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get",headers:['X-Roles':'role-3, role-2, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get",headers:['X-Roles':'role-3, role-2, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 3)
    }

    def "when request is for api validator, should increment ApiValidator mbeans for all"() {
        given:
        def validator1Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target
        def validatorAllTarget = repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count")
        validatorAllTarget = (validatorAllTarget == null) ? 0 : validatorAllTarget

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post",headers:['X-Roles':'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_ALL, "Count") == (validatorAllTarget + 3)
    }
}
