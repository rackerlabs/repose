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
import spock.lang.Specification

@org.junit.experimental.categories.Category(Slow.class)
class StartWithZeroNodesTest extends Specification {

    int targetPort
    Deproxy deproxy
    Endpoint endpoint

    int port

    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeValveLauncher repose

    int sleep_duration = 35000

    def setup() {

        properties = new TestProperties()
        targetPort = properties.targetPort
        deproxy = new Deproxy()
        endpoint = deproxy.addEndpoint(targetPort)

        port = properties.reposePort

        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

        def params = properties.getDefaultTemplateParams()
        params += [
                'host': 'example.com',
                'port': port,
        ]

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        reposeConfigProvider.applyConfigs("features/core/valveSelfConfigure/zero-nodes", params)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                "http://localhost:${port}",
                properties.getConfigDirectory(),
                port
        )
        repose.enableDebug()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        sleep(sleep_duration)
    }

    def "when we start with zero nodes in the system model, then switch to a system model with one  localhost node"() {

        def mc

        when: "Repose first starts up with zero nodes"
        deproxy.makeRequest(url: "http://localhost:${port}")
        then: "it should not connect"
        thrown(ConnectException)



        when: "change the configs while it's running - add a single localhost node"
        def params = properties.getDefaultTemplateParams()
        params += [
                'host': 'localhost',
                'port': port,
        ]
        reposeConfigProvider.applyConfigs('features/core/valveSelfConfigure/one-node', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "Repose reloads the configs"
        mc = deproxy.makeRequest(url: "http://localhost:${port}")
        then: "the first node should be available"
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
