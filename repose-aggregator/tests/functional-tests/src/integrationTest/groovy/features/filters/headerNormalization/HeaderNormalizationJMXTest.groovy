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
import spock.lang.Unroll

@Category(Slow.class)
class HeaderNormalizationJMXTest extends ReposeValveTest {

    String PREFIX = "${jmxHostname}-metrics:type=meters,name=\"org.openrepose.filters.headernormalization.HeaderNormalizationFilter.Normalization"

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
        reposeConfigProvider.applyConfigs("features/filters/headerNormalization", params)
    }

    @Unroll
    def "when a client makes requests, jmx should keep accurate #reqres counts"() {

        given:
        String HEADER_NORMALIZATION_ROOT_GET = "${PREFIX}.${reqres}.GET..\\*\""
        String HEADER_NORMALIZATION_ROOT_POST = "${PREFIX}.${reqres}.POST..\\*\""
        String HEADER_NORMALIZATION_ROOT_PUT = "${PREFIX}.${reqres}.POST..\\*\""
        String HEADER_NORMALIZATION_RESOURCE_POST = "${PREFIX}.${reqres}.POST./resource/(.\\*)\""
        String HEADER_NORMALIZATION_RESOURCE_PUT = "${PREFIX}.${reqres}.PUT./resource/(.\\*)\""
        String HEADER_NORMALIZATION_SERVERS_GET = "${PREFIX}.${reqres}.GET./servers/(.\\*)\""
        String HEADER_NORMALIZATION_SERVERS_POST = "${PREFIX}.${reqres}.POST./servers/(.\\*)\""
        String HEADER_NORMALIZATION_SERVERS_PUT = "${PREFIX}.${reqres}.PUT./servers/(.\\*)\""
        reposeConfigProvider.applyConfigs("features/filters/headerNormalization/metrics/${reqres}", params)
        repose.start()

        when:
        def mc = deproxy.makeRequest(url: urlBase, method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: urlBase, method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_POST, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: urlBase, method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_PUT, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_POST, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/resource/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_RESOURCE_PUT, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_GET, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_POST, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${urlBase}/servers/1243/", method: "PUT")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SERVERS_PUT, "Count") == 1

        where:
        reqres << ["request", "response"]
    }

    def "when multiple filter instances are configured, each should add to the count"() {

        given:
        String reqres = "request"
        String HEADER_NORMALIZATION_ROOT_GET = "${PREFIX}.${reqres}.GET..\\*\""
        String HEADER_NORMALIZATION_SECONDARY_PATH_GET = "${PREFIX}.${reqres}.GET./secondary/path/(.\\*)\""
        String HEADER_NORMALIZATION_TERTIARY_PATH_GET = "${PREFIX}.${reqres}.GET./tertiary/path/(.\\*)\""
        reposeConfigProvider.applyConfigs("features/filters/headerNormalization/metrics/multiple", params)
        repose.start()

        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: urlBase)

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") ?: 0) == 0
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0

        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${urlBase}/secondary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0

        when: "client makes a request that matches two filters' uri-regex attributes"
        mc = deproxy.makeRequest(url: "${urlBase}/tertiary/path/asdf")

        then:
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(HEADER_NORMALIZATION_TERTIARY_PATH_GET, "Count") == 2
    }

    def cleanup() {
        repose.stop()
        deproxy.shutdown()
    }

}
