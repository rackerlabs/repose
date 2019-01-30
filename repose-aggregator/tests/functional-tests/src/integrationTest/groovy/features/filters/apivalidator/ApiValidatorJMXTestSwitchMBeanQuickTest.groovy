/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.spockframework.runtime.SpockAssertionError
import scaffold.category.XmlParsing
import spock.util.concurrent.PollingConditions

/**
 * This is running the same test as the ApiValidatorJMXTestSwitchMBeanTest, but I'm using the short
 * circuit method instead, so hopefully it'll be more stable, and maybe pass
 */
@Category(XmlParsing)
class ApiValidatorJMXTestSwitchMBeanQuickTest extends ReposeValveTest {

    final def conditions = new PollingConditions(timeout: 10, initialDelay: 3)

    String validatorBeanDomain = 'com.rackspace.com.papi.components.checker:*'
    String validatorClassName = "com.rackspace.com.papi.components.checker.Validator"

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

        String ConfigurationBeanDomain = '*:001="org",002="openrepose",003="core",004="services",005="jmx",006="ConfigurationInformation"'
        String ConfigurationClassName = "org.openrepose.core.services.jmx.ConfigurationInformation"

        deproxy.makeRequest(url: reposeEndpoint + "/")

        when:
        def validatorBeans = repose.jmx.getMBeans(ConfigurationBeanDomain, ConfigurationClassName, 1)

        then:
        validatorBeans.size() == 1

    }

    def "when loading validators on startup, should register validator MXBeans"() {

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/")

        then:
        def foundBeans = []
        int lastFoundSize = 0
        boolean foundOne = false
        boolean foundTwo = false
        boolean foundThree = false
        int upperLoopCount = 0
        try {
            conditions.eventually {
                upperLoopCount += 1
                foundBeans = repose.jmx.quickMBeans(validatorBeanDomain, validatorClassName)
                lastFoundSize = foundBeans.size()
                assert lastFoundSize == 3
                //Assert that we found our three beans!
                foundOne = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-1")
                }
                foundTwo = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-2")
                }
                foundThree = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-3")
                }
                assert foundOne
                assert foundTwo
                assert foundThree
            }
        } catch (IllegalArgumentException iae) {
            throw new SpockAssertionError("Spock Timeout: With $upperLoopCount tries. Run Assertions: Total Beans:$lastFoundSize foundOne:$foundOne, foundTwo:$foundTwo, foundThree:$foundThree  :  $foundBeans", iae)
        }
    }

    def "when reconfiguring validators from 3 to 2, should drop 3 MXBeans and register 2"() {

        when: "I make a request to exercise repose so it has the jmx beeeens"
        deproxy.makeRequest(url: reposeEndpoint + "/")

        then: "I get beans before making the config change"
        def foundBeans = []
        int lastFoundSize = 0
        boolean foundOne = false
        boolean foundTwo = false
        boolean foundThree = false
        int upperLoopCount = 0
        try {
            conditions.eventually {
                upperLoopCount += 1
                foundBeans = repose.jmx.quickMBeans(validatorBeanDomain, validatorClassName)
                lastFoundSize = foundBeans.size()
                assert lastFoundSize == 3
                //Assert that we found our three beans!
                foundOne = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-1")
                }
                foundTwo = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-2")
                }
                foundThree = foundBeans.any { bean ->
                    bean.objectName.toString().contains("role-3")
                }
                assert foundOne
                assert foundTwo
                assert foundThree
            }
        } catch (IllegalArgumentException iae) {
            throw new SpockAssertionError("Spock Timeout: With $upperLoopCount tries. Run Assertions: Total Beans:$lastFoundSize foundOne:$foundOne, foundTwo:$foundTwo, foundThree:$foundThree  :  $foundBeans", iae)
        }


        when: "I update the Repose API Validator filter with 2 new validators"
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmxupdate", params, /*sleepTime*/ 25)


        then: "Repose has 2 validator MBeans, and they are not the same beans as before the update"
        def loopCount = 0
        def lastSawAfterUpdateBeansCount = 0
        def lastSawAfterUpdateBeans = []
        try {
            conditions.eventually {
                deproxy.makeRequest(url: reposeEndpoint + "/")
                loopCount += 1
                //The new mbeans should be different, and we should always have two
                def afterUpdateBeans = repose.jmx.quickMBeans(validatorBeanDomain, validatorClassName)
                lastSawAfterUpdateBeans = afterUpdateBeans
                lastSawAfterUpdateBeansCount = afterUpdateBeans.size()
                assert afterUpdateBeans.size() == 2
                //Look for the two beans
                assert afterUpdateBeans.any { bean -> bean.objectName.toString().contains("role-a") }
                assert afterUpdateBeans.any { bean -> bean.objectName.toString().contains("role-b") }
            }
        } catch (IllegalArgumentException iae) {
            //Stupid spock is stupid and I don't know why
            throw new SpockAssertionError("Timeout: ${loopCount} tries. With ${lastSawAfterUpdateBeansCount} beans last seen: ${lastSawAfterUpdateBeans}", iae)
        }
    }
}
