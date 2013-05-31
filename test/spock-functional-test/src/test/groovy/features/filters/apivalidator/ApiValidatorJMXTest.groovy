package features.filters.apivalidator
import framework.ReposeValveTest
import framework.client.jmx.JmxClient

class ApiValidatorJMXTest extends ReposeValveTest {

    def JmxClient jmxClient

    def validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    def validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setup() {
        reposeConfigProvider.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/jmx")

        reposeLauncher.enableJmx(true)
        reposeLauncher.start()

        jmxClient = new JmxClient(properties.getProperty("repose.jmxUrl"))
    }

    def cleanup() {
        reposeLauncher.stop()
    }

    def "when loading validators, should register MXBeans"() {

        when:
        reposeClient.doGet("/", ['X-Roles': "role-1, role-2, role-3"])
        def totalMXBeans = jmxClient.getMBeanCount(validatorBeanDomain, validatorClassName, 3)

        then:
        totalMXBeans == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        when:
        reposeClient.doGet("/", ['X-Roles': "role-1, role-2, role-3"])
        def totalMXBeans = jmxClient.getMBeanCount(validatorBeanDomain, validatorClassName, 3)
        totalMXBeans == 3

        and:
        reposeConfigProvider.updateConfigs("features/filters/apivalidator/jmxupdate")
        reposeClient.doGet("/", ['X-Roles': "role-a, role-b"])
        totalMXBeans = jmxClient.getMBeanCount(validatorBeanDomain, validatorClassName, 2)

        then:
        totalMXBeans == 2
    }

}
