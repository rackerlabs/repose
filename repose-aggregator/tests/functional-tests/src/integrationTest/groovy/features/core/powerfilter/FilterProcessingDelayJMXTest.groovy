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
class FilterProcessingDelayJMXTest extends ReposeValveTest {
    private static final String KEY_PROPERTIES_PREFIX =
        /001="org",002="openrepose",003="core",004="FilterProcessingTime",005="Delay"/
    private static final String API_VALIDATOR_MBEAN_NAME = /006="api-validator"/
    private static final String IP_IDENTITY_MBEAN_NAME = /006="ip-user"/
    private static final List<String> TIMER_DOUBLE_ATTR_NAMES =
        ["50thPercentile", "75thPercentile", "95thPercentile", "98thPercentile", "99thPercentile", "999thPercentile",
         "OneMinuteRate", "FiveMinuteRate", "FifteenMinuteRate", "Min", "Max", "Mean", "MeanRate", "StdDev"]
    private static final List<String> TIMER_STRING_ATTR_NAMES = ["DurationUnit", "RateUnit"]

    private static String apiValidatorMetric
    private static String ipIdentityMetric

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/multifilters", params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        apiValidatorMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$API_VALIDATOR_MBEAN_NAME"
        ipIdentityMetric = "$jmxHostname:$KEY_PROPERTIES_PREFIX,$IP_IDENTITY_MBEAN_NAME"
    }

    def "when a request is sent through Repose, metrics should be recorded for each filter"() {
        given:
        def apiValidatorInitialCount = repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorMetric)
        def ipIdentityInitialCount = repose.jmx.getMBeanCountAttributeWithWaitForNonZero(ipIdentityMetric)

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/resource", method: "GET", headers: ["X-Roles": "role-1"])

        then: "the request count has been incremented"
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(ipIdentityMetric) == ipIdentityInitialCount + 1
        repose.jmx.getMBeanCountAttributeWithWaitForNonZero(apiValidatorMetric) == apiValidatorInitialCount + 1

        and: "the other attributes containing a double value are populated with a non-negative value"
        TIMER_DOUBLE_ATTR_NAMES.each { attr ->
            assert (repose.jmx.getMBeanAttribute(ipIdentityMetric, attr) as double) >= 0.0
            assert (repose.jmx.getMBeanAttribute(apiValidatorMetric, attr) as double) >= 0.0
        }

        and: "the other attributes containing a string value are populated with a non-empty value"
        TIMER_STRING_ATTR_NAMES.each { attr ->
            assert !(repose.jmx.getMBeanAttribute(ipIdentityMetric, attr) as String).isEmpty()
            assert !(repose.jmx.getMBeanAttribute(apiValidatorMetric, attr) as String).isEmpty()
        }
    }
}
