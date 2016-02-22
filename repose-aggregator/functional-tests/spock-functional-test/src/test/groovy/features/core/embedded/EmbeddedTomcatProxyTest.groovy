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
package features.core.embedded

import framework.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Specification

class EmbeddedTomcatProxyTest extends Specification {

    static ReposeLauncher repose
    static Deproxy deproxy
    static String tomcatEndpoint

    def setupSpec() {
        def TestProperties properties = new TestProperties()
        ReposeLogSearch log = new ReposeLogSearch(properties.logFile);
        log.cleanLog()
        int originServicePort = properties.targetPort
        deproxy = new Deproxy()
        deproxy.addEndpoint(originServicePort)

        int reposePort = properties.reposePort
        tomcatEndpoint = "http://localhost:${reposePort}"

        def configDirectory = properties.getConfigDirectory()
        def configTemplates = properties.getRawConfigDirectory()
        def rootWar = properties.getReposeRootWar()
        def buildDirectory = properties.getReposeHome() + "/.."
        def mocksWar = properties.getMocksWar()
        def mocksPath = mocksWar.substring(mocksWar.lastIndexOf('/') + 1, mocksWar.lastIndexOf('.'))

        ReposeConfigurationProvider config = new ReposeConfigurationProvider(configDirectory, configTemplates)

        def params = properties.getDefaultTemplateParams()
        params += [
                'reposePort'             : reposePort,
                'targetPort'             : originServicePort,
                'repose.config.directory': configDirectory,
                'repose.cluster.id'      : "repose1",
                'repose.node.id'         : 'node1',
                'appPath'                : mocksPath
        ]
        config.cleanConfigDirectory()
        config.applyConfigs("common", params)
        config.applyConfigs("features/filters/ipidentity", params)

        config.applyConfigs("features/core/embedded", params)

        repose = new ReposeContainerLauncher(config, properties.getTomcatJar(), "repose1", "node1", rootWar, reposePort, mocksWar)
        repose.enableDebug()
        repose.clusterId = "repose"
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
        MessageChain mc = deproxy.makeRequest(url: tomcatEndpoint + "/cluster?a=b&c=123", headers: ['passheader': 'value1', 'PassHeAder': 'value2'])
        def xmlData = new XmlSlurper().parseText(mc.receivedResponse.body.toString())

        then: "Repose Should Forward Response"
        mc.receivedResponse.code == "200"

        and: "Response should contain a body"
        !mc.receivedResponse.body.toString().empty

        and: "Repose should have passed the pass header"
        xmlData.headers.header.find { it.@name == 'passheader' }.@value.text() == "value1"
        xmlData.headers.header.findAll { it.@name == 'passheader' }.size() == 2

        and: "Repose should have passed query params"
        xmlData."query-params".parameter.find { it.@name == "a" }.@value.text() == "[b]"
        xmlData."query-params".parameter.size() == 2
    }
}
