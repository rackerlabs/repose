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
package features.core.valveSelfConfigure

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification


@org.junit.experimental.categories.Category(Slow.class)
class MultiClusterMultiNodeTest extends Specification {

    int targetPort1
    int targetPort2
    Deproxy deproxy
    Endpoint endpoint1
    Endpoint endpoint2

    int port11
    int port12
    int port21
    int port22
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose
    Map params = [:]

    def setup() {

        properties = new TestProperties()

        targetPort1 = properties.targetPort
        targetPort2 = properties.targetPort2
        deproxy = new Deproxy()
        endpoint1 = deproxy.addEndpoint(targetPort1)
        endpoint2 = deproxy.addEndpoint(targetPort2)

        port11 = properties.reposePort
        port12 = PortFinder.Singleton.getNextOpenPort()
        port21 = PortFinder.Singleton.getNextOpenPort()
        port22 = PortFinder.Singleton.getNextOpenPort()


        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        params = properties.getDefaultTemplateParams()
        params += [
                'port11': port11,
                'port12': port12,
                'port21': port21,
                'port22': port22,
        ]
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/two-clusters-two-nodes-each", params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port11}",
                properties.getConfigDirectory(),
                port11
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${port11}")
        repose.waitForNon500FromUrl("http://localhost:${port21}")
    }

    def "check that nodes are either available or not depending on hostname"() {

        def mc

        when: "send a request to node-1-1"
        mc = deproxy.makeRequest(url: "http://localhost:${port11}")

        then: "Repose forawrds the request"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == endpoint1


        when: "try to send a request to node-1-2"
        mc = deproxy.makeRequest(url: "http://localhost:${port12}")

        then:
        thrown(ConnectException)


        when: "send a request to node-2-1"
        mc = deproxy.makeRequest(url: "http://localhost:${port21}")

        then: "Repose forawrds the request"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].endpoint == endpoint2


        when: "try to send a request to node-2-2"
        mc = deproxy.makeRequest(url: "http://localhost:${port22}")

        then:
        thrown(ConnectException)
    }

    def cleanup() {

        if (repose) {
            repose.stop()
        }
        if (deproxy) {
            deproxy.shutdown()
        }
    }
}

