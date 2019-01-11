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
package features.services.metrics

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Slow

@Category(Slow.class)
class MetricsEnableDisableTest extends ReposeValveTest {

    private static final String KEY_PROPERTIES_PREFIX = /001="org",002="openrepose"/
    private static final String DESTINATION_ROUTER_MBEAN_PART =
        /003="filters",004="destinationrouter",005="DestinationRouterFilter",006="Routed Response"/
    private static final String ENDPOINT_MBEAN_NAME = /007="endpoint"/
    private static final String ALL_MBEAN_NAME = /007="ACROSS ALL"/
    private static final String REPOSE_RESPONSE_CODE_2XX_SUFFIX = /003="core",004="ResponseCode",005="Repose",006="2XX"/

    private static Map params
    private static String destinationRouterEndpointMetric
    private static String destinationRouterAllEndpointMetric
    private static String reposeResponseCode2xxMetric

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        destinationRouterEndpointMetric =
            "$jmxHostname:$KEY_PROPERTIES_PREFIX,$DESTINATION_ROUTER_MBEAN_PART,$ENDPOINT_MBEAN_NAME"
        destinationRouterAllEndpointMetric =
            "$jmxHostname:$KEY_PROPERTIES_PREFIX,$DESTINATION_ROUTER_MBEAN_PART,$ALL_MBEAN_NAME"
        reposeResponseCode2xxMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$REPOSE_RESPONSE_CODE_2XX_SUFFIX"
    }

    def setup() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
    }

    def cleanup() {
        repose?.stop()
    }

    def "when metrics are enabled, reporting should occur"() {
        given: "Repose is started with configuration that enables metrics"
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsenabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterEndpointMetric) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(reposeResponseCode2xxMetric) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterAllEndpointMetric) == 1
    }

    def "when metrics are disabled, reporting should not occur"() {
        given: "Repose is started with configuration that disables metrics"
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsdisabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanCountAttribute(destinationRouterEndpointMetric) == 0
        repose.jmx.getMBeanCountAttribute(reposeResponseCode2xxMetric) == 0
        repose.jmx.getMBeanCountAttribute(destinationRouterAllEndpointMetric) == 0
    }

    def "when 'enabled' is not specified, reporting should occur"() {
        given: "Repose is started with configuration that does not specify if metrics should be enabled nor disabled"
        repose.configurationProvider.applyConfigs("features/services/metrics/notspecified", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterEndpointMetric) == 1
    }

    def "when metrics config is missing, reporting should occur"() {
        given: "Repose is started with no metrics related configuration"
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(destinationRouterEndpointMetric) == 1
    }
}
