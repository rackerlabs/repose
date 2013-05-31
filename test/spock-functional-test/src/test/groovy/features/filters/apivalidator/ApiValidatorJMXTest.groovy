package features.filters.apivalidator
import framework.ReposeValveTest

class ApiValidatorJMXTest extends ReposeValveTest {

    def validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    def validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setup() {
        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/jmx")
        repose.enableJmx(true)
        repose.setJmxUrl(properties.getProperty("repose.jmxUrl"))
        repose.start()
    }

    def cleanup() {
        repose.stop()
    }

    def "when loading validators, should register MXBeans"() {

        when:
        deproxy.doGet("/", ['X-Roles': "role-1, role-2, role-3"])
        def totalMXBeans = repose.jmx.getMBeanCount(validatorBeanDomain, validatorClassName, 3)

        then:
        totalMXBeans == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        when:
        deproxy.doGet("/", ['X-Roles': "role-1, role-2, role-3"])
        def totalMXBeans = repose.jmx.getMBeanCount(validatorBeanDomain, validatorClassName, 3)
        totalMXBeans == 3

        and:
        repose.updateConfigs("features/filters/apivalidator/jmxupdate")
        deproxy.doGet("/", ['X-Roles': "role-a, role-b"])
        totalMXBeans = repose.jmx.getMBeanCount(validatorBeanDomain, validatorClassName, 2)

        then:
        totalMXBeans == 2
    }

}
