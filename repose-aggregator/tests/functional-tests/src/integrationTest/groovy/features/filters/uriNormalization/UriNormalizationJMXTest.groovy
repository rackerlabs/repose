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
package features.filters.uriNormalization

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

@Category(Slow.class)
class UriNormalizationJMXTest extends ReposeValveTest {

    String PREFIX = "${jmxHostname}-metrics:type=meters,name=\"org.openrepose.filters.urinormalization.UriNormalizationFilter.Normalization"

    String URI_NORMALIZATION_ROOT_GET = "${PREFIX}.GET..\\*\""
    String URI_NORMALIZATION_ROOT_POST = "${PREFIX}.POST..\\*\""
    String URI_NORMALIZATION_RESOURCE_GET = "${PREFIX}.GET./resource/.\\*\""
    String URI_NORMALIZATION_RESOURCE_POST = "${PREFIX}.POST./resource/.\\*\""
    String URI_NORMALIZATION_SERVERS_GET = "${PREFIX}.GET./servers/.\\*\""
    String URI_NORMALIZATION_SERVERS_POST = "${PREFIX}.POST./servers/.\\*\""
    String URI_NORMALIZATION_SECONDARY_PATH_GET = "${PREFIX}.GET./secondary/path/.\\*\""
    String URI_NORMALIZATION_TERTIARY_PATH_GET = "${PREFIX}.GET./tertiary/path/.\\*\""

    Map params
    ReposeConfigurationProvider reposeConfigProvider

    def setup() {

        // get ports
        properties = new TestProperties()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        // configure and start repose
        def targetHostname = properties.getTargetHostname()

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                properties.reposeEndpoint,
                properties.getConfigDirectory(),
                properties.reposePort
        )
        repose.enableDebug()

        params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)
    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/uriNormalization/metrics/single", params)
        repose.start()
        sleep(30000)

        when:
        def mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_POST, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/resource/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_RESOURCE_GET, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/resource/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_RESOURCE_POST, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/servers/1243?a=1", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SERVERS_GET, "Count") == 1

        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/servers/1243?a=1", method: "POST")

        then:
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SERVERS_POST, "Count") == 1
    }

    def "when multiple filter instances are configured, each should add to the count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/uriNormalization/metrics/multiple", params)
        repose.start()
        sleep(30000)

        when: "client makes a request that matches one filter's uri-regex attribute"
        def mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") ?: 0) == 0
        (repose.jmx.quickMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0


        when: "client makes a request that matches two filters' uri-regex attributes (1 & 2)"
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/secondary/path/asdf?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        (repose.jmx.quickMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") ?: 0) == 0


        when: "client makes a request that matches two filters' uri-regex attributes (1 & 3)"
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/tertiary/path/asdf?a=1")

        then:
        mc.receivedResponse.code == "200"
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_ROOT_GET, "Count") == 2
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_SECONDARY_PATH_GET, "Count") == 1
        repose.jmx.getMBeanAttribute(URI_NORMALIZATION_TERTIARY_PATH_GET, "Count") == 2
    }

    def cleanup() {
        if (repose && repose.isUp()) {
            repose.stop()
        }

        if (deproxy) {
            deproxy.shutdown()
        }
    }
}
