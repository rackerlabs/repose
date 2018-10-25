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
package features.filters.headerNormalization

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

@Category(Slow.class)
class HeaderNormalizationJMXTest extends ReposeValveTest {

    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="filters",004="headernormalization",005="HeaderNormalizationFilter",006="Normalization"/
    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static Map params
    private static String jmxPrefix

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        jmxPrefix = "$jmxHostname:$KEY_PROPERTIES_PREFIX"
    }

    def setup() {
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/headerNormalization", params)
    }

    def cleanup() {
        repose.stop()
    }

    @Unroll
    def "when a client makes requests, jmx should keep accurate #reqres counts"() {
        given: "Repose is started with configuration that enables metrics for the request/response (depending on the test)"
        repose.configurationProvider.applyConfigs("features/filters/headerNormalization/metrics/$reqres", params)
        repose.start()

        and: "the following strings will be used in the JMX reported metrics"
        String headerNormRootGet = $/$jmxPrefix,007="$reqres",008="GET",009="_\*"/$
        String headerNormRootPost = $/$jmxPrefix,007="$reqres",008="POST",009="_\*"/$
        String headerNormRootPut = $/$jmxPrefix,007="$reqres",008="PUT",009="_\*"/$
        String headerNormResourcePost = $/$jmxPrefix,007="$reqres",008="POST",009="/resource/(_\*)"/$
        String headerNormResourcePut = $/$jmxPrefix,007="$reqres",008="PUT",009="/resource/(_\*)"/$
        String headerNormServersGet = $/$jmxPrefix,007="$reqres",008="GET",009="/servers/(_\*)"/$
        String headerNormServersPost = $/$jmxPrefix,007="$reqres",008="POST",009="/servers/(_\*)"/$
        String headerNormServersPut = $/$jmxPrefix,007="$reqres",008="PUT",009="/servers/(_\*)"/$
        String headerNormAll = $/$jmxPrefix,007="$reqres",008="ACROSS ALL"/$

        when:
        deproxy.makeRequest(url: reposeEndpoint, method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 1

        when:
        deproxy.makeRequest(url: reposeEndpoint, method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootPost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 2

        when:
        deproxy.makeRequest(url: reposeEndpoint, method: "PUT")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootPut) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 3

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/resource/1243/", method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormResourcePost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 5

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/resource/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormResourcePut) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 7

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243/", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormServersGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 9

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243/", method: "POST")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormServersPost) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 11

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/servers/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormServersPut) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 13

        where:
        reqres << ["request", "response"]
    }

    def "when multiple filter instances are configured, each should add to the count"() {
        given: "Repose is started with three Header Normalization filters in the filter chain, each with their own configuration"
        repose.configurationProvider.applyConfigs("features/filters/headerNormalization/metrics/multiple", params)
        repose.start()

        and: "the following strings will be used in the JMX reported metrics"
        String headerNormRootGet = $/$jmxPrefix,007="request",008="GET",009="_\*"/$
        String headerNormSecondaryPathGet = $/$jmxPrefix,007="request",008="GET",009="/secondary/path/(_\*)"/$
        String headerNormTertiaryPathGet = $/$jmxPrefix,007="request",008="GET",009="/tertiary/path/(_\*)"/$
        String headerNormAll = $/$jmxPrefix,007="request",008="ACROSS ALL"/$

        when: "client makes a request that matches one filter's uri-regex attribute"
        deproxy.makeRequest(url: reposeEndpoint)

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootGet) == 1
        repose.jmx.getMBeanCountAttribute(headerNormSecondaryPathGet) == 0
        repose.jmx.getMBeanCountAttribute(headerNormTertiaryPathGet) == 0
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 1

        when: "client makes a request that matches filters one and two's uri-regex attributes"
        deproxy.makeRequest(url: "$reposeEndpoint/secondary/path/asdf")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootGet) == 3
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormSecondaryPathGet) == 1
        repose.jmx.getMBeanCountAttribute(headerNormTertiaryPathGet) == 0
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 4

        when: "client makes a request that matches filters one and three's uri-regex attributes"
        deproxy.makeRequest(url: "$reposeEndpoint/tertiary/path/asdf")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormRootGet) == 5
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormSecondaryPathGet) == 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormTertiaryPathGet) == 2
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(headerNormAll) == 8

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(headerNormRootGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(headerNormSecondaryPathGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(headerNormTertiaryPathGet, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(headerNormAll, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(headerNormRootGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(headerNormSecondaryPathGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(headerNormTertiaryPathGet, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(headerNormAll, METER_STRING_ATTR_NAME) as String).isEmpty()
    }
}
