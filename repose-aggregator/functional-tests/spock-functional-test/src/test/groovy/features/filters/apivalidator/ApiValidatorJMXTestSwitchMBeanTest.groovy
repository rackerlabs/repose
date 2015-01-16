package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.spockframework.runtime.SpockAssertionError
import spock.lang.Shared
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

@Category(Slow.class)
class ApiValidatorJMXTestSwitchMBeanTest extends ReposeValveTest {

    final def conditions = new PollingConditions(timeout: 10, initialDelay: 1)

    //Have to configure this with logic to get the hostname so that JMX works
    @Shared
    String PREFIX = "\"${jmxHostname}-org.openrepose.core.filters\":type=\"ApiValidator\",scope=\""

    String validatorBeanDomain = '\"com.rackspace.com.papi.components.checker\":*'
    String validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

    def setupSpec() {
        //Nothing
    }

    def cleanupSpec() {
        //AlsoNothing
    }

    def setup() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory() //Ensure it's clean!!!1
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmx", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    static def params

    def cleanup() {
        deproxy.shutdown()
        repose.stop()
    }


    def "when loading validators on startup, should register Configuration MXBeans"() {

        String ConfigurationBeanDomain = "*org.openrepose.core.services.jmx:type=ConfigurationInformation"
        String ConfigurationClassName = "org.openrepose.core.services.jmx.ConfigurationInformation"

        deproxy.makeRequest(url: reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(ConfigurationBeanDomain, ConfigurationClassName, 1)

        then:
        validatorBeans.size() == 1

    }

    def "when loading validators on startup, should register validator MXBeans"() {

        deproxy.makeRequest(url: reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        then:
        validatorBeans.size() == 3
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        deproxy.makeRequest(url: reposeEndpoint + "/")

        given:
        def beforeUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 3)

        when: "I update the Repose API Validator filter with 2 new validators"
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmxupdate", params, /*sleepTime*/ 25)

        then: "Repose has 2 validator MBeans, and they are not the same beans as before the update"
        def loopCount = 0
        def lastSawAfterUpdateBeansCount = 0
        def lastSawAfterUpdateBeans = []
        try {
            conditions.eventually {
                loopCount += 1
                //The new mbeans should be different, and we should always have two
                def afterUpdateBeans = repose.jmx.getMBeans(validatorBeanDomain, validatorClassName, 2)
                lastSawAfterUpdateBeans = afterUpdateBeans
                lastSawAfterUpdateBeansCount = afterUpdateBeans.size()
                assert afterUpdateBeans.size() == 2
                afterUpdateBeans.each { updatedBean ->
                    beforeUpdateBeans.each {
                        assert (updatedBean.name != it.name)
                    }
                }
            }
        } catch (IllegalArgumentException iae) {
            //Stupid spock is stupid and I don't know why
            throw new SpockAssertionError("Stupid failure: ${loopCount} tries. With ${lastSawAfterUpdateBeansCount} beans last seen: ${lastSawAfterUpdateBeans}", iae)
        }
    }
}
