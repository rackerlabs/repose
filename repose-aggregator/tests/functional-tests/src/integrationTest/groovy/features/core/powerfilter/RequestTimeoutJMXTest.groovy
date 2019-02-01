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
import org.rackspace.deproxy.Response
import scaffold.category.Core

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT

@Category(Core)
class RequestTimeoutJMXTest extends ReposeValveTest {
    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="core",004="RequestTimeout",005="TimeoutToOrigin"/
    private static final String ALL_ENDPOINTS_MBEAN_NAME = /006="All Endpoints"/
    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static String timeoutToRootPathEndpointMetric
    private static String timeoutToAllEndpointsMetric

    def handlerTimeout = { new Response(SC_REQUEST_TIMEOUT, 'WIZARD FAIL') }

    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/common", params)
        repose.start()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        timeoutToRootPathEndpointMetric =
            $/$jmxHostname:$KEY_PROPERTIES_PREFIX,006="localhost:${properties.targetPort}/root_path"/$
        timeoutToAllEndpointsMetric = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$ALL_ENDPOINTS_MBEAN_NAME/
    }

    def "when responses have timed out, should increment RequestTimeout mbeans for specific endpoint"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(timeoutToRootPathEndpointMetric)

        when: "two requests are made that will timeout in the origin service"
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)

        then: "the two requests are reflected in the endpoint metric"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(timeoutToRootPathEndpointMetric) == target + 2

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(timeoutToRootPathEndpointMetric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(timeoutToRootPathEndpointMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }

    def "when responses have timed out, should increment RequestTimeout mbeans for all endpoint"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(timeoutToAllEndpointsMetric)

        when: "two requests are made that will timeout in the origin service"
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)

        then: "the two requests are reflected in the all endpoints metric"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(timeoutToAllEndpointsMetric) == target + 2

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(timeoutToAllEndpointsMetric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(timeoutToAllEndpointsMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }

    def "when SOME responses have timed out, should increment RequestTimeout mbeans for specific endpoint only for timeouts"() {
        given:
        def target = repose.jmx.getMBeanCountAttribute(timeoutToAllEndpointsMetric)

        when: "two of the four requests will timeout in the origin service"
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint", defaultHandler: handlerTimeout)
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint")

        then: "the two requests that timed out are reflected in the all endpoints metric"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(timeoutToAllEndpointsMetric) == target + 2

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(timeoutToAllEndpointsMetric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(timeoutToAllEndpointsMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }
}
