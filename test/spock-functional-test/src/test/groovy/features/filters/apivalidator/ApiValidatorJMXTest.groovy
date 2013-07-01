package features.filters.apivalidator
import framework.ReposeValveTest
import org.rackspace.gdeproxy.Deproxy

class ApiValidatorJMXTest extends ReposeValveTest {

    String validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    String validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setup() {
        repose.applyConfigs(
                "features/filters/apivalidator/common",
                "features/filters/apivalidator/jmx")
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        deproxy.makeRequest(reposeEndpoint + "/")
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        sleep(4000) //TODO: add a clean way to ensure deproxy has really shutdown all endpoints
        repose.stop()
    }

    def "when loading validators on startup, should register validator MXBeans"() {

        when:
        def validatorBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        then:
        validatorBeans.size() == 3

    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

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

}