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
package features.core.powerfilter

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

@Category(Slow.class)
class DestinationRouterJMXTest extends ReposeValveTest {

    String PREFIX = "\"${jmxHostname}-org.openrepose.core.filters\":type=\"DestinationRouter\",scope=\""
    String NAME_TARGET = "\",name=\"endpoint\""
    String NAME_TARGET_ALL = "\",name=\"ACROSS ALL\""

    String DESTINATION_ROUTER_TARGET = PREFIX + "destination-router" + NAME_TARGET
    String DESTINATION_ROUTER_ALL = PREFIX + "destination-router" + NAME_TARGET_ALL

    def setup() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanup() {
        if (deproxy)
            deproxy.shutdown()
        repose.stop()
    }

    def "when requests match destination router target URI, should increment DestinationRouter mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/1"])
        deproxy.makeRequest([url: reposeEndpoint + "/cluster"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == (target + 1)
    }


    def "when requests match destination router target URI, should increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(DESTINATION_ROUTER_ALL, "Count")
        target = (target == null) ? 0 : target

        when:
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint2/2"])
        deproxy.makeRequest([url: reposeEndpoint + "/endpoint/2"])

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == (target + 2)
    }

    def "when requests DO NOT match destination router target URI, should NOT increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.quickMBeanAttribute(DESTINATION_ROUTER_ALL, "Count")
        target = (target == null) ? 0 : target


        when:
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])
        deproxy.makeRequest([url: reposeEndpoint + "/non-existing"])

        then:
        repose.jmx.quickMBeanAttribute(DESTINATION_ROUTER_ALL, "Count") == target
    }
}
