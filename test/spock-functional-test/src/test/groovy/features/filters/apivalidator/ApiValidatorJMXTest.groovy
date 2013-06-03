package features.filters.apivalidator
import framework.ReposeValveTest

class ApiValidatorJMXTest extends ReposeValveTest {

    def validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    def validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setup() {
        repose.applyConfigs("features/filters/apivalidator/common", "features/filters/apivalidator/jmx")
        repose.enableJmx(true)
        repose.start()
    }

    def cleanup() {
        repose.stop()
    }

    def "when loading validators on startup, should register validator MXBeans"() {

        when:
        def validatorBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        then:
        validatorBeans.size() == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        when:
        def beforeUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)
        beforeUpdateBeans.size() == 3

        and:
        repose.updateConfigs("features/filters/apivalidator/jmxupdate")
        def afterUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 2)

        then:
        afterUpdateBeans.size() == 2
        afterUpdateBeans.each { updatedBean ->
            beforeUpdateBeans.each {
                updatedBean.name != it.name
            }
        }
    }


}
