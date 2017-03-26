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

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy

/**
 * This test ensures that the versioning filter provides metrics via JMX,
 * counting how many requests it services and which endpoints it sends them to.
 *
 * http://wiki.openrepose.org/display/REPOSE/Repose+JMX+Metrics+Development
 *
 */

@Category(Slow.class)
class VersioningJMXTest extends ReposeValveTest {

    String PREFIX = "${jmxHostname}:001=\"org\",002=\"openrepose\",003=\"filters\",004=\"versioning\",005=\"VersioningFilter\",006=\"VersionedRequest\""

    String VERSION_UNVERSIONED = "${PREFIX},007=\"Unversioned\""
    String VERSION_V1 = "${PREFIX},007=\"v1\""
    String VERSION_V2 = "${PREFIX},007=\"v2\""

    Map params
    ReposeConfigurationProvider reposeConfigProvider

    def setup() {
        properties = new TestProperties()

        // start deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        deproxy.addEndpoint(properties.targetPort2)

        // configure and start repose
        reposeConfigProvider = new ReposeConfigurationProvider(properties.configDirectory, properties.configTemplates)

        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.reposeJar,
                properties.reposeEndpoint,
                properties.configDirectory,
                properties.reposePort
        )
        repose.enableDebug()

        params = properties.getDefaultTemplateParams()
        reposeConfigProvider.applyConfigs("common", params)

    }

    def "when a client makes requests, jmx should keep accurate count"() {

        given:
        reposeConfigProvider.applyConfigs("features/filters/versioning/metrics", params)
        repose.start()
        sleep(30000)



        when:
        def mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        (repose.jmx.quickMBeanAttribute(VERSION_V1, "Count") ?: 0) == 0
        (repose.jmx.quickMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/v1/resource", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V1, "Count") == 1
        (repose.jmx.quickMBeanAttribute(VERSION_V2, "Count") ?: 0) == 0


        when:
        mc = deproxy.makeRequest(url: "${properties.reposeEndpoint}/v2/resource", method: "GET")

        then:
        repose.jmx.getMBeanAttribute(VERSION_UNVERSIONED, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V1, "Count") == 1
        repose.jmx.getMBeanAttribute(VERSION_V2, "Count") == 1

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
