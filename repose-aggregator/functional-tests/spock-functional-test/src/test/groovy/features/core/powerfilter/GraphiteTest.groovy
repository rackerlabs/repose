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

import framework.ReposeValveTest
import framework.mocks.MockGraphite
import groovy.util.logging.Log4j
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder

@Log4j
class GraphiteTest extends ReposeValveTest {

    static String METRIC_PREFIX = "test.1.metrics"
    static String METRIC_NAME = "org.openrepose.core.ResponseCode.Repose.2XX.count"

    int graphitePort;
    MockGraphite mockGraphite
    int lastCount = -1

    def setup() {

        graphitePort = PortFinder.Singleton.getNextOpenPort()

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        lastCount = -1
        def lineProc = { line ->
            def m = (line =~ /${METRIC_PREFIX}\.${jmxHostname}-${METRIC_NAME}\s+(\d+)/)
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

    def cleanup() {
        if (deproxy) {
            deproxy.shutdown()
        }
        if (repose) {
            repose.stop()
        }
        if (mockGraphite) {
            mockGraphite.stop()
        }
    }

    def "when sending requests, data should be logged to graphite"() {

        when:
        def mc1 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc2 = deproxy.makeRequest(url: reposeEndpoint + "/endpoint")
        def mc3 = deproxy.makeRequest(url: reposeEndpoint + "/cluster")
        sleep(2000)

        then:
        lastCount == 3
        mc1.receivedResponse.code == "200"
        mc2.receivedResponse.code == "200"
        mc3.receivedResponse.code == "200"
    }
}
