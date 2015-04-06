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

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

@Category(Slow.class)
class HeaderNormalizationJMXTest extends ReposeValveTest {

    String PREFIX = "\"${jmxHostname}-org.openrepose.core.filters\":type=\"HeaderNormalization\",scope=\"header-normalization\""

    String HEADER_NORMALIZATION_ROOT_GET = "${PREFIX},name=\".\\*_GET\""
    String HEADER_NORMALIZATION_ROOT_POST = "${PREFIX},name=\".\\*_POST\""
    String HEADER_NORMALIZATION_ROOT_PUT = "${PREFIX},name=\".\\*_POST\""
    String HEADER_NORMALIZATION_RESOURCE_POST = "${PREFIX},name=\"/resource/(.\\*)_POST\""
    String HEADER_NORMALIZATION_RESOURCE_PUT = "${PREFIX},name=\"/resource/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SERVERS_GET = "${PREFIX},name=\"/servers/(.\\*)_GET\""
    String HEADER_NORMALIZATION_SERVERS_POST = "${PREFIX},name=\"/servers/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SERVERS_PUT = "${PREFIX},name=\"/servers/(.\\*)_POST\""
    String HEADER_NORMALIZATION_SECONDARY_PATH_GET = "${PREFIX},name=\"/secondary/path/(.\\*)_GET\""
    String HEADER_NORMALIZATION_TERTIARY_PATH_GET = "${PREFIX},name=\"/tertiary/path/(.\\*)_GET\""
    String HEADER_NORMALIZATION_ACROSS_ALL = "${PREFIX},name=\"ACROSS ALL\""

    int reposePort
    int originServicePort
    String urlBase
    Map params
    ReposeConfigurationProvider reposeConfigProvider

    def setup() {

        properties = new TestProperties()

        // get ports
        reposePort = properties.reposePort
        originServicePort = properties.targetPort

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        // configure and start repose

        urlBase = properties.reposeEndpoint

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                urlBase,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()

        params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)

    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/headerNormalization/metrics/single", params)
        repose.start()


        when:
        def mc = deproxy.makeRequest(url: urlBase, method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: urlBase, method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 2

        when:
        mc = deproxy.makeRequest(url: urlBase, method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 3

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 4

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 5

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 6

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_POST, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 7

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_PUT, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 8

    }

    def "when multiple filter instances are configured, each should add to the count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/headerNormalization/metrics/multiple", params)
        repose.start()

        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: urlBase)

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") ?: 0) == 0
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 1

        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${urlBase}/secondary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 3


        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${urlBase}/tertiary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ACROSS_ALL, "Count") == 5

    }

    def cleanup() {
        repose.stop()
        deproxy.shutdown()
    }

}
