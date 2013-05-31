package features.filters.apivalidator

import framework.ReposeValveTest
import framework.client.http.HttpRequestParams
import framework.client.jmx.JmxClient

class ApiValidatorJMXTest extends ReposeValveTest {

    def X_ROLES = "X-Roles"
    def JmxClient jmxClient
    def HttpRequestParams requestParams

    def validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    def validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setup() {
        reposeConfigProvider.applyConfigurations("features/filters/apivalidator/common", "features/filters/apivalidator/jmx")

        reposeLauncher.enableJmx(true)
        reposeLauncher.start()

        jmxClient = new JmxClient(properties.getProperty("repose.jmxUrl"))

        requestParams = new HttpRequestParams()
        requestParams.headers.put(X_ROLES, "role-1, role-2, role-3")
    }

    def cleanup() {
        reposeLauncher.stop()
    }

    def "when loading validators, should register MXBeans"() {

        when:
        reposeClient.doGet("/", requestParams)
        def totalMXBeans = jmxClient.verifyMBeanCount(validatorBeanDomain, validatorClassName, 3)

        then:
        totalMXBeans == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        when:
        reposeClient.doGet("/", requestParams)
        def totalMXBeans = jmxClient.verifyMBeanCount(validatorBeanDomain, validatorClassName, 3)
        totalMXBeans == 3

        and:
        reposeConfigProvider.updateConfigurations("features/filters/apivalidator/jmxupdate")
        requestParams.headers.put(X_ROLES, "role-a, role-b")
        reposeClient.doGet("/", requestParams)
        totalMXBeans = jmxClient.verifyMBeanCount(validatorBeanDomain, validatorClassName, 2)

        then:
        totalMXBeans == 2
    }

}
