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

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

class MetricsEnableDisableTest extends ReposeValveTest {

    String PREFIX = "\"${jmxHostname}-org.openrepose.core.filters\":type=\"DestinationRouter\",scope=\""
    String RESPONSE_CODE_PREFIX = "\"${jmxHostname}-org.openrepose.core\":type=\"ResponseCode\",scope=\""

    String NAME_TARGET = "\",name=\"endpoint\""
    String NAME_2XX = "\",name=\"2XX\""
    String REPOSE_2XX = RESPONSE_CODE_PREFIX + "Repose" + NAME_2XX
    String ALL_ENDPOINTS_2XX = RESPONSE_CODE_PREFIX + "All Endpoints" + NAME_2XX

    String DESTINATION_ROUTER_TARGET = PREFIX + "destination-router" + NAME_TARGET

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanup() {

        repose.stop()

    }

    def cleanupSpec() {

        deproxy.shutdown()
    }

    def "when metrics are enabled, reporting should occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsenabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
        repose.jmx.getMBeanAttribute(REPOSE_2XX, "Count") == 1
        repose.jmx.getMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == 1
    }

    def "when metrics are disabled, reporting should not occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/metricsdisabled", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.quickMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == null
        repose.jmx.quickMBeanAttribute(REPOSE_2XX, "Count") == null
        repose.jmx.quickMBeanAttribute(ALL_ENDPOINTS_2XX, "Count") == null
    }

    def "when 'enabled' is not specified, reporting should occur"() {

        setup: "load the correct configuration file"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/notspecified", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
    }

    def "when metrics config is missing, reporting should occur"() {

        setup: "only load the common configuration files"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/metrics/common", params)
        repose.start()

        when:
        deproxy.makeRequest(url: reposeEndpoint + "/endpoint/1")

        then:
        repose.jmx.getMBeanAttribute(DESTINATION_ROUTER_TARGET, "Count") == 1
    }

}
