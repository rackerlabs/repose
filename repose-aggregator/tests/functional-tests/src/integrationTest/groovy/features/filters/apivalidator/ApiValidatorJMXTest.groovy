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

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import spock.lang.Shared

@Category(Slow.class)
class ApiValidatorJMXTest extends ReposeValveTest {

    //Have to configure this with logic to get the hostname so that JMX works
    @Shared
    String PREFIX = "${jmxHostname}-metrics:type=meters,name=\"org.openrepose.filters.apivalidator.ApiValidatorHandler.invalid-request"

    String NAME_ROLE_1 = "role-1"
    String NAME_ROLE_2 = "role-2"
    String NAME_ROLE_3 = "role-3"

    String API_VALIDATOR_1 = PREFIX + "." + NAME_ROLE_1 + "\""
    String API_VALIDATOR_2 = PREFIX + "." + NAME_ROLE_2 + "\""
    String API_VALIDATOR_3 = PREFIX + "." + NAME_ROLE_3 + "\""

    def setupSpec() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory() //Ensure this crap is cleaned up
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jmx", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        repose.waitForNon500FromUrl(properties.reposeEndpoint)
    }

    static def params

    def "when request is for role-1, should increment invalid request for ApiValidator mbeans for role 1"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post", headers: ['X-Roles': 'role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
    }

    def "when request is for role-2, should increment invalid request for ApiValidator mbeans for role 2"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post", headers: ['X-Roles': 'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
    }

    def "when request is for role-3, should increment invalid request for ApiValidator mbeans for role 3"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get", headers: ['X-Roles': 'role-3']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
    }

    def "when request is for role-3 and role-2, should increment invalid request for ApiValidator mbeans for role 3 and role 2"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-3, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get", headers: ['X-Roles': 'role-3, role-2']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count") == ((validator1Target == 0) ? null : validator1Target)
    }

    def "when request is for role-3 and role-1, should increment invalid request for ApiValidator mbeans for role 3 and role 1"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-3, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get", headers: ['X-Roles': 'role-3, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count") == ((validator2Target == 0) ? null : validator2Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
    }

    def "when request is for role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 1 and role 2"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-1, role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get", headers: ['X-Roles': 'role-1, role-2']])

        then:
        repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count") == ((validator3Target == 0) ? null : validator3Target)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
    }

    def "when request is for role-3, role-1 and role-2, should increment invalid request for ApiValidator mbeans for role 3, role 1, and role 2"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "get", headers: ['X-Roles': 'role-3, role-2, role-1']])
        deproxy.makeRequest([url: reposeEndpoint + "/non-resource", method: "get", headers: ['X-Roles': 'role-3, role-2, role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
    }

    def "when request is for api validator, should increment ApiValidator mbeans for all"() {
        given:
        def validator1Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_1, "Count")
        validator1Target = (validator1Target == null) ? 0 : validator1Target
        def validator2Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_2, "Count")
        validator2Target = (validator2Target == null) ? 0 : validator2Target
        def validator3Target = repose.jmx.quickMBeanAttribute(API_VALIDATOR_3, "Count")
        validator3Target = (validator3Target == null) ? 0 : validator3Target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post", headers: ['X-Roles': 'role-3']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post", headers: ['X-Roles': 'role-2']])
        deproxy.makeRequest([url: reposeEndpoint + "/resource", method: "post", headers: ['X-Roles': 'role-1']])

        then:
        repose.jmx.getMBeanAttribute(API_VALIDATOR_3, "Count") == (validator3Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_2, "Count") == (validator2Target + 1)
        repose.jmx.getMBeanAttribute(API_VALIDATOR_1, "Count") == (validator1Target + 1)
    }
}
