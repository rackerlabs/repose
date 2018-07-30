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
package features.services.datastore

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.*
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.spockframework.runtime.SpockAssertionError
import scaffold.category.Intense
import spock.lang.Specification

import java.util.concurrent.TimeUnit

@Category(Intense.class)
class DistDatastoreServiceClientAuthFullTest extends Specification {
    static String reposeEndpoint1
    static String reposeEndpoint2

    static Deproxy deproxy
    static ReposeLauncher repose1
    static ReposeLauncher repose2

    def setupSpec() {
        def properties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))
        def reposeLogSearch = new ReposeLogSearch(properties.logFile)

        def reposePort1 = properties.reposePort
        def reposePort2 = PortFinder.instance.getNextOpenPort()
        def dataStorePort1 = PortFinder.instance.getNextOpenPort()
        def dataStorePort2 = PortFinder.instance.getNextOpenPort()

        reposeEndpoint1 = "http://localhost:${reposePort1}"
        reposeEndpoint2 = "http://localhost:${reposePort2}"

        def params = properties.getDefaultTemplateParams()
        params += [
                'reposePort1'      : reposePort1,
                'reposePort2'      : reposePort2,
                'repose.cluster.id': 'repose1',
                'repose.node.id'   : 'node1',
                'datastorePort1'   : dataStorePort1,
                'datastorePort2'   : dataStorePort2
        ]

        def configTemplates = properties.getConfigTemplates()
        def configDirectory = properties.getConfigDirectory()
        def config = new ReposeConfigurationProvider(configDirectory, configTemplates)
        config.applyConfigs("common", params)
        config.applyConfigs("features/services/datastore/multinode", params)
        config.applyConfigs("features/services/datastore/multinode/clientauth", params)

        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def tomcatJar = properties.getTomcatJar()
        def rootWar = properties.getReposeRootWar()

        repose1 = new ReposeContainerLauncher(config, tomcatJar, "repose1", "node1", rootWar, reposePort1)
        repose1.enableDebug()
        repose1.start()
        reposeLogSearch.awaitByString("repose1:node1 -- Repose ready", 1, 60, TimeUnit.SECONDS)

        repose2 = new ReposeContainerLauncher(config, tomcatJar, "repose1", "node2", rootWar, reposePort2)
        repose2.enableDebug()
        repose2.start()
        reposeLogSearch.awaitByString("repose1:node2 -- Repose ready", 1, 60, TimeUnit.SECONDS)
    }

    def "Test repose container with multi-nodes"() {
        given:
        MessageChain mc
        def user = UUID.randomUUID().toString();

        // This tests rate limit share between 2 nodes which is accomplished using the dist datastore.
        when: "the request hit the first node using up all limit"
        3.times {
            mc = deproxy.makeRequest(url: reposeEndpoint1 + "/test", headers: ['X-PP-USER': user])
            if (mc.receivedResponse.code != "200") {
                throw new SpockAssertionError("Expected 200 response from repose but was " + mc.receivedResponse.code)
            }
        }
        mc = deproxy.makeRequest(url: reposeEndpoint2 + "/test", headers: ['X-PP-USER': user])

        then: "the request hit second node will be rate limit"
        mc.receivedResponse.code == "413"
    }

    def cleanupSpec() {
        repose2?.stop()
        repose1?.stop()
        deproxy?.shutdown()
    }
}
