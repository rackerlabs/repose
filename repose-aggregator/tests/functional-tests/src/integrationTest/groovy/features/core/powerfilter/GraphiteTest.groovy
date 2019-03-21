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
import org.linkedin.util.clock.SystemClock
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockGraphite
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition

@Category(Core)
class GraphiteTest extends ReposeValveTest {

    static final String METRIC_PREFIX = "test\\.1\\.metrics"
    static final String METRIC_NAME = "org\\.openrepose\\.core\\.ResponseCode\\.Repose\\.2XX\\.count"

    static int lastCount
    static MockGraphite mockGraphite

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def graphitePort = PortFinder.instance.getNextOpenPort()
        def lineProc = { line ->
            def m = (line =~ /${METRIC_PREFIX}\.${jmxHostname}\.${METRIC_NAME}\s+(\d+)/)
            if (m) {
                lastCount = m.group(1).toInteger()
            }
        }
        mockGraphite = new MockGraphite(graphitePort, lineProc)

        def params = properties.getDefaultTemplateParams() + [graphitePort: graphitePort]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/graphite", params)
        repose.start()
    }

    def setup() {
        lastCount = 0
    }

    def cleanupSpec() {
        mockGraphite?.stop()
    }

    def "when sending requests, data should be logged to graphite"() {
        when:
        def mc1 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc2 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc3 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")

        then:
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        mc3.receivedResponse.code == "200"
        waitForCondition(new SystemClock(), '2s', '250', {
            lastCount == 3
        })
    }
}
