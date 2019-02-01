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
package features.filters.urinormalization

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Filters)
class UriNormalizationJMXTest extends ReposeValveTest {

    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="filters",004="urinormalization",005="UriNormalizationFilter",006="Normalization"/
    private static final String URI_NORM_ROOT_GET_NAME = /007="GET",008="_\*"/
    private static final String URI_NORM_ROOT_POST_NAME = /007="POST",008="_\*"/
    private static final String URI_NORM_RESOURCE_GET_NAME = $/007="GET",008="/resource/_\*"/$
    private static final String URI_NORM_RESOURCE_POST_NAME = $/007="POST",008="/resource/_\*"/$
    private static final String URI_NORM_SERVERS_GET_NAME = $/007="GET",008="/servers/_\*"/$
    private static final String URI_NORM_SERVERS_POST_NAME = $/007="POST",008="/servers/_\*"/$
    private static final String URI_NORM_SECONDARY_PATH_GET_NAME = $/007="GET",008="/secondary/path/_\*"/$
    private static final String URI_NORM_TERTIARY_PATH_GET_NAME = $/007="GET",008="/tertiary/path/_\*"/$
    private static final String URI_NORM_ACROSS_ALL_NAME = /007="ACROSS ALL"/

    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static String uriNormRootGet
    private static String uriNormSecondaryPathGet
    private static String uriNormTertiaryPathGet
    private static String uriNormAllEndpoints

    private static Map params

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        uriNormRootGet = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_ROOT_GET_NAME/
        uriNormSecondaryPathGet = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_SECONDARY_PATH_GET_NAME/
        uriNormTertiaryPathGet = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_TERTIARY_PATH_GET_NAME/
        uriNormAllEndpoints = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_ACROSS_ALL_NAME/
    }

    def setup() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
    }

    def cleanup() {
        repose.stop()
    }

    def "when a client makes requests, jmx should keep accurate count"() {
        given: "Repose is started with one instance of the URI Normalization filter in the filter chain"
        repose.configurationProvider.applyConfigs("features/filters/uriNormalization/metrics/single", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        and: "the following strings will be used in the JMX reported metrics"
        String uriNormRootPost = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_ROOT_POST_NAME/
        String uriNormResourceGet = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_RESOURCE_GET_NAME/
        String uriNormResourcePost = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_RESOURCE_POST_NAME/
        String uriNormServersGet = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_SERVERS_GET_NAME/
        String uriNormServersPost = /$jmxHostname:$KEY_PROPERTIES_PREFIX,$URI_NORM_SERVERS_POST_NAME/

        and: "the initial values are known for the metrics potentially affected by the startup checking requests"
        int rootGetTarget = repose.jmx.getMBeanCountAttribute(uriNormRootGet)
        int allEndpointsTarget = repose.jmx.getMBeanCountAttribute(uriNormAllEndpoints)

        when:
        deproxy.makeRequest(url: "$reposeEndpoint?a=1", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormRootGet) == rootGetTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 1

        when:
        deproxy.makeRequest(url: "$reposeEndpoint?a=1", method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormRootPost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 2

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/resource/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormResourceGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 3

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/resource/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormResourcePost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 4

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormServersGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 5

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormServersPost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 6

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243?a=1", method: "PUT")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 7
    }

    def "when multiple filter instances are configured, each should add to the count"() {
        given: "Repose is started with three instances of the URI Normalization filter in the filter chain"
        repose.configurationProvider.applyConfigs("features/filters/uriNormalization/metrics/multiple", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)

        and: "the initial values are known for the metrics potentially affected by the startup checking requests"
        int rootGetTarget = repose.jmx.getMBeanCountAttribute(uriNormRootGet)
        int allEndpointsTarget = repose.jmx.getMBeanCountAttribute(uriNormAllEndpoints)

        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: "$reposeEndpoint?a=1")

        then:
        mc.receivedResponse.code as Integer == SC_OK
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormRootGet) == rootGetTarget + 1
        repose.jmx.getMBeanCountAttribute(uriNormSecondaryPathGet) == 0
        repose.jmx.getMBeanCountAttribute(uriNormTertiaryPathGet) == 0
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 1

        when: "client makes a request that matches two filters' uri-regex attributes (1 & 2)"
        mc = deproxy.makeRequest(url: "$reposeEndpoint/secondary/path/asdf?a=1")

        then:
        mc.receivedResponse.code as Integer == SC_OK
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormRootGet) == rootGetTarget + 2
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormSecondaryPathGet) == 1
        repose.jmx.getMBeanCountAttribute(uriNormTertiaryPathGet) == 0
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 3

        when: "client makes a request that matches two filters' uri-regex attributes (1 & 3)"
        mc = deproxy.makeRequest(url: "$reposeEndpoint/tertiary/path/asdf?a=1")

        then:
        mc.receivedResponse.code as Integer == SC_OK
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormRootGet) == rootGetTarget + 2
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormSecondaryPathGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormTertiaryPathGet) == 2
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(uriNormAllEndpoints) == allEndpointsTarget + 5

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(uriNormRootGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(uriNormSecondaryPathGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(uriNormTertiaryPathGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(uriNormAllEndpoints, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(uriNormRootGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(uriNormSecondaryPathGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(uriNormTertiaryPathGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(uriNormAllEndpoints, METER_STRING_ATTR_NAME) as String).isEmpty()
    }
}
