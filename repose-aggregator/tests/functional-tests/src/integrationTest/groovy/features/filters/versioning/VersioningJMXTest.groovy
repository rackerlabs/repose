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
package features.filters.versioning

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

/**
 * This test ensures that the versioning filter provides metrics via JMX,
 * counting how many requests it services and which endpoints it sends them to.
 */

@Category(Filters)
class VersioningJMXTest extends ReposeValveTest {

    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="filters",004="versioning",005="VersioningFilter",006="VersionedRequest"/
    private static final String VERSION_UNVERSION_NAME = /007="Unversioned"/
    private static final String VERSION_V1_NAME = /007="v1"/
    private static final String VERSION_V2_NAME = /007="v2"/
    private static final List<String> METER_DOUBLE_ATTR_NAMES =
        ["OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "MeanRate"]
    private static final String METER_STRING_ATTR_NAME = "RateUnit"

    private static String versionUnversionedMetric
    private static String versionV1Metric
    private static String versionV2Metric

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        deproxy.addEndpoint(properties.targetPort2)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/versioning/metrics", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)

        versionUnversionedMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$VERSION_UNVERSION_NAME"
        versionV1Metric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$VERSION_V1_NAME"
        versionV2Metric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$VERSION_V2_NAME"
    }

    def "when a client makes requests, jmx should keep accurate count"() {
        given: "the initial values are known for the metrics potentially affected by the startup checking requests"
        int versionUnversionedTarget = repose.jmx.getMBeanCountAttribute(versionUnversionedMetric)
        int versionV1Target = repose.jmx.getMBeanCountAttribute(versionV1Metric)
        int versionV2Target = repose.jmx.getMBeanCountAttribute(versionV2Metric)

        when:
        deproxy.makeRequest(url: reposeEndpoint, method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionUnversionedMetric) == versionUnversionedTarget + 1
        repose.jmx.getMBeanCountAttribute(versionV1Metric) == versionV1Target
        repose.jmx.getMBeanCountAttribute(versionV2Metric) == versionV2Target

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/v1/resource", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionUnversionedMetric) == versionUnversionedTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionV1Metric) == versionV1Target + 1
        repose.jmx.getMBeanCountAttribute(versionV2Metric) == versionV2Target

        when:
        deproxy.makeRequest(url: "$reposeEndpoint/v2/resource", method: "GET")

        then:
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionUnversionedMetric) == versionUnversionedTarget + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionV1Metric) == versionV1Target + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(versionV2Metric) == versionV2Target + 1

        and: "the other attributes containing a double value are populated with a non-negative value"
        METER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(versionUnversionedMetric, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(versionV1Metric, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(versionV2Metric, attr) as double) >= 0.0
        }

        and: "the other attribute containing a string value is populated with a non-empty value"
        !(repose.jmx.getMBeanAttribute(versionUnversionedMetric, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(versionV1Metric, METER_STRING_ATTR_NAME) as String).isEmpty()
        !(repose.jmx.getMBeanAttribute(versionV2Metric, METER_STRING_ATTR_NAME) as String).isEmpty()
    }
}
