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
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import framework.PortFinder
import spock.lang.Specification

@Category(Slow.class)
class RuntimeSysmodChangesTest extends Specification {

    int targetPort
    Deproxy deproxy
    Endpoint endpoint

    int port1
    int port2
    int port3
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    //This must be longer than the 15 second hard coded Poller timeout
    //TODO: eventually replace this with a JMX trigger to force a configuration update.
    int sleep_duration = 35000

    def setup() {

        properties = new TestProperties()

        targetPort = properties.targetPort
        deproxy = new Deproxy()
        endpoint = deproxy.addEndpoint(targetPort)

        port1 = properties.reposePort
        port2 = PortFinder.instance.getNextOpenPort()
        port3 = PortFinder.instance.getNextOpenPort()

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        def params = properties.defaultTemplateParams
        params += [
                'port1'     : port1,
                'port2'     : port2,
                'port3'     : port3,

                'proto'     : 'http',
                'targetPort': targetPort,
                'sysmodPort': port1,

        ]
        reposeConfigProvider.cleanConfigDirectory()

        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/single-node-with-proto", params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port1}",
                properties.getConfigDirectory(),
                port1
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl("http://localhost:${port1}")
    }

    def "when making runtime changes to the system model, available nodes/ports/etc should change accordingly"() {

        def mc

        when: "Repose first starts up"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "Repose first starts up"
        deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "port 2 should not connect"
        thrown(ConnectException)

        when: "Repose first starts up"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two nodes"
        def params = properties.getDefaultTemplateParams()
        params += [
                'targetPort': targetPort,
                'node1host' : 'localhost',
                'node2host' : 'localhost',
                'node1port' : port1,
                'node2port' : port2,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/two-nodes', params)
        println("Change config to two-nodes")
        sleep(sleep_duration)
        repose.waitForNon500FromUrl("http://localhost:${port1}")
        repose.waitForNon500FromUrl("http://localhost:${port2}")
        then:
        1 == 1 //WAT


        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - one node on port 2"
        params = properties.getDefaultTemplateParams()
        params += [
                'proto'     : 'http',
                'targetPort': targetPort,
                'sysmodPort': port2,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/single-node-with-proto', params)
        println("changed configs to single-node-with-proto")
        sleep(sleep_duration)
        repose.waitForNon500FromUrl("http://localhost:${port2}")
        then:
        1 == 1



        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "port 1 should not connect"
        thrown(ConnectException)

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two of three nodes"
        params = properties.getDefaultTemplateParams()
        params += [
                'targetPort': targetPort,
                'node1host' : 'localhost',
                'node2host' : 'localhost',
                'node3host' : 'example.com',
                'node1port' : port1,
                'node2port' : port2,
                'node3port' : port3,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/three-nodes', params)
        println("changed to three-nodes config")
        sleep(sleep_duration)
        repose.waitForNon500FromUrl("http://localhost:${port1}")
        repose.waitForNon500FromUrl("http://localhost:${port2}")
        then:
        1 == 1


        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "port 3 should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - two of three nodes again, but different hostnames"
        params = properties.getDefaultTemplateParams()
        params += [
                'targetPort': targetPort,
                'node1host' : 'example.com',
                'node2host' : 'localhost',
                'node3host' : 'localhost',
                'node1port' : port1,
                'node2port' : port2,
                'node3port' : port3,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/three-nodes', params)
        println("changed to three-nodes config again, but a different hostname")
        sleep(sleep_duration)
        repose.waitForNon500FromUrl("http://localhost:${port2}")
        repose.waitForNon500FromUrl("http://localhost:${port3}")
        then:
        1 == 1



        when: "configs have changed"
        deproxy.makeRequest(url: "http://localhost:${port1}")
        then: "port 1 should not connect"
        thrown(ConnectException)

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port2}")
        then: "node 2 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1

        when: "configs have changed"
        mc = deproxy.makeRequest(url: "http://localhost:${port3}")
        then: "node 3 should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
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
