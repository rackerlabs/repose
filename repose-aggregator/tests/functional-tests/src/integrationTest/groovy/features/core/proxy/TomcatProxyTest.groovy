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
package features.core.proxy

import framework.ReposeConfigurationProvider
import framework.ReposeContainerLauncher
import framework.ReposeLauncher
import framework.TestProperties
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Specification

class TomcatProxyTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {

        TestProperties properties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))

        int originServicePort = properties.targetPort
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = properties.reposePort
        tomcatEndpoint = properties.reposeEndpoint

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getConfigTemplates()
        def rootWar = properties.getReposeRootWar()
        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configTemplates)

        def params = properties.getDefaultTemplateParams()
        params += [
                'repose.cluster.id': "repose1",
                'repose.node.id'   : 'node1',
                'targetHostname'   : 'localhost',
        ]
        config.applyConfigs("features/core/proxy", params)
        config.applyConfigs("common", params)


        repose = new ReposeContainerLauncher(config, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort)
        repose.enableDebug()
        repose.clusterId = "repose"
        repose.nodeId = "simple-node"
        repose.start()
        repose.waitForNon500FromUrl(tomcatEndpoint, 120)
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    def "Should Pass Requests through repose"() {

        when: "Request is sent through Repose/Tomcat"
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster", headers: ['x-trace-request': 'true', 'x-pp-user': 'usertest1'])

        then: "Repose Should Forward Request"
        mc.handlings[0].request.getHeaders().contains("x-pp-user")

        and: "Repose Should Forward Response"
        mc.receivedResponse.code == "200"
    }
}
