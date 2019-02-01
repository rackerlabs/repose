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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core

@Category(Core)
class DestinationRouterJMXTest extends ReposeValveTest {
    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="filters",004="destinationrouter",005="DestinationRouterFilter",006="Routed Response"/
    private static final String ENDPOINT_MBEAN_NAME = /007="endpoint"/
    private static final String ALL_MBEAN_NAME = /007="ACROSS ALL"/
    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static String destinationRouterEndpointMetric
    private static String destinationRouterAllMetric

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        destinationRouterEndpointMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$ENDPOINT_MBEAN_NAME"
        destinationRouterAllMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$ALL_MBEAN_NAME"

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()
    }

    def "when requests match destination router target URI, should increment DestinationRouter mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(destinationRouterEndpointMetric)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")
        deproxy.makeRequest(url: reposeEndpoint + "/cluster")

        then: "the endpoint metric only goes up by one since the call to /cluster should not count"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterEndpointMetric) == target + 1

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(destinationRouterEndpointMetric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(destinationRouterEndpointMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }

    def "when requests match destination router target URI, should increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(destinationRouterAllMetric)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint2/2")
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/2")

        then: "the all metric should go up by two"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterAllMetric) == target + 2

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(destinationRouterAllMetric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(destinationRouterAllMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }

    def "when requests DO NOT match destination router target URI, should NOT increment DestinationRouter mbeans for all endpoints"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(destinationRouterAllMetric)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/non-existing")
        deproxy.makeRequest(url: reposeEndpoint + "/non-existing")

        then: "the value did not change"
        repose.jmx.getMBeanCountAttribute(destinationRouterAllMetric) == target
    }
}
