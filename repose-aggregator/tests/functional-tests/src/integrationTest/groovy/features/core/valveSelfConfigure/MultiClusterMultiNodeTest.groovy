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

import framework.PortFinder
import framework.ReposeValveTest
import framework.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint

@org.junit.experimental.categories.Category(Slow.class)
class MultiClusterMultiNodeTest extends ReposeValveTest {

    Endpoint endpoint1
    Endpoint endpoint2

    int port11
    int port12
    int port21
    int port22
    Map params = [:]

    def setup() {

        deproxy = new Deproxy()
        endpoint1 = deproxy.addEndpoint(properties.targetPort)
        endpoint2 = deproxy.addEndpoint(properties.targetPort2)

        port11 = properties.reposePort
        port12 = PortFinder.instance.getNextOpenPort()
        port21 = PortFinder.instance.getNextOpenPort()
        port22 = PortFinder.instance.getNextOpenPort()


        params = properties.getDefaultTemplateParams()
        params += [
                'port11': port11,
                'port12': port12,
                'port21': port21,
                'port22': port22,
        ]
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/two-clusters-two-nodes-each", params)

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
}

