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

import framework.ReposeValveTest
import framework.category.Slow
import org.rackspace.deproxy.Deproxy

@org.junit.experimental.categories.Category(Slow.class)
class StartWithZeroNodesTest extends ReposeValveTest {

    int port

    int sleep_duration = 35000

    def setup() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        port = properties.reposePort

        def params = properties.getDefaultTemplateParams()
        params += [
                'host': 'example.com',
                'port': port,
        ]

        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/common", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/container-no-port", params)
        repose.configurationProvider.applyConfigs("features/core/valveSelfConfigure/zero-nodes", params)
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
        repose.configurationProvider.applyConfigs('features/core/valveSelfConfigure/one-node', params)
        sleep(sleep_duration)
        then:
        1 == 1



        when: "Repose reloads the configs"
        mc = deproxy.makeRequest(url: "http://localhost:${port}")
        then: "the first node should be available"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
    }
}
