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
import scaffold.category.XmlParsing

@Category(XmlParsing)
class ApiValidatorJMXTest extends ReposeValveTest {
    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="filters",004="apivalidator",005="ApiValidatorHandler",006="invalid-request"/
    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static final String ROLE_ALL = "ACROSS ALL"

    private static String apiValidator1
    private static String apiValidator2
    private static String apiValidator3
    private static String apiValidatorAll

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmx", params)
        repose.start()

        repose.waitForNon500FromUrl(properties.reposeEndpoint)

        apiValidator1 = /$jmxHostname:$KEY_PROPERTIES_PREFIX,007="role-1"/
        apiValidator2 = /$jmxHostname:$KEY_PROPERTIES_PREFIX,007="role-2"/
        apiValidator3 = /$jmxHostname:$KEY_PROPERTIES_PREFIX,007="role-3"/
        apiValidatorAll = /$jmxHostname:$KEY_PROPERTIES_PREFIX,007="$ROLE_ALL"/
    }

    def "when request is for role-1, should increment invalid request for ApiValidator mbeans for role 1"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST", headers: ['X-Roles': 'role-1'])
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-1'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator1) == validator1Target + 1
        repose.jmx.getMBeanCountAttribute(apiValidator2) == validator2Target
        repose.jmx.getMBeanCountAttribute(apiValidator3) == validator3Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 1
    }

    def "when request is for role-2, should increment invalid request for ApiValidator mbeans for role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST", headers: ['X-Roles': 'role-2'])
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-2'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator2) == validator2Target + 1
        repose.jmx.getMBeanCountAttribute(apiValidator1) == validator1Target
        repose.jmx.getMBeanCountAttribute(apiValidator3) == validator3Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 1
    }

    def "when request is for role-3, should increment invalid request for ApiValidator mbeans for role 3"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-3'])
        deproxy.makeRequest(url: reposeEndpoint + "/non-resource", method: "GET", headers: ['X-Roles': 'role-3'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator3) == validator3Target + 1
        repose.jmx.getMBeanCountAttribute(apiValidator2) == validator2Target
        repose.jmx.getMBeanCountAttribute(apiValidator1) == validator1Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 1
    }

    def "when request is for role-3 and role-2, should increment invalid request for ApiValidator mbeans for role 3 and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-3, role-2'])
        deproxy.makeRequest(url: reposeEndpoint + "/non-resource", method: "GET", headers: ['X-Roles': 'role-3, role-2'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator3) == validator3Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator2) == validator2Target + 1
        repose.jmx.getMBeanCountAttribute(apiValidator1) == validator1Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 2
    }

    def "when request is for role-3 and role-1, should increment invalid request for ApiValidator mbeans for role 3 and role 1"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-3, role-1'])
        deproxy.makeRequest(url: reposeEndpoint + "/non-resource", method: "GET", headers: ['X-Roles': 'role-3, role-1'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator3) == validator3Target + 1
        repose.jmx.getMBeanCountAttribute(apiValidator2) == validator2Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator1) == validator1Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 2
    }

    def "when request is for role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 1 and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-1, role-2'])
        deproxy.makeRequest(url: reposeEndpoint + "/non-resource", method: "GET", headers: ['X-Roles': 'role-1, role-2'])

        then:
        repose.jmx.getMBeanCountAttribute(apiValidator3) == validator3Target
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator2) == validator2Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator1) == validator1Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 2
    }

    def "when request is for role-3, role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 3, role 1, and role 2"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ['X-Roles': 'role-3, role-2, role-1'])
        deproxy.makeRequest(url: reposeEndpoint + "/non-resource", method: "GET", headers: ['X-Roles': 'role-3, role-2, role-1'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator3) == validator3Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator2) == validator2Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator1) == validator1Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 3
    }

    def "when request is for api validator, should increment ApiValidator mbeans for all"() {
        given:
        def validator1Target = repose.jmx.getMBeanCountAttribute(apiValidator1)
        def validator2Target = repose.jmx.getMBeanCountAttribute(apiValidator2)
        def validator3Target = repose.jmx.getMBeanCountAttribute(apiValidator3)
        def validatorAllTarget = repose.jmx.getMBeanCountAttribute(apiValidatorAll)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST", headers: ['X-Roles': 'role-3'])
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST", headers: ['X-Roles': 'role-2'])
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "POST", headers: ['X-Roles': 'role-1'])

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator1) == validator1Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator2) == validator2Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidator3) == validator3Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorAll) == validatorAllTarget + 3

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(apiValidator1, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(apiValidator2, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(apiValidator3, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(apiValidatorAll, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(apiValidator1, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(apiValidator2, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(apiValidator3, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(apiValidatorAll, METER_STRING_ATTR_NAME) as String).isEmpty()
    }
}
