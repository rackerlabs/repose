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
package features.core.configloadingandreloading

import framework.ReposeConfigurationProvider
import framework.ReposeLogSearch
import framework.ReposeValveLauncher
import framework.TestProperties
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.PortFinder
import spock.lang.Specification
import spock.lang.Unroll

@Category(Slow.class)
class StartWithGoodConfigsTest extends Specification {

    int reposePort
    int targetPort
    String url
    TestProperties properties
    ReposeConfigurationProvider reposeConfigProvider
    ReposeLogSearch reposeLogSearch
    ReposeValveLauncher repose
    Map params = [:]
    Deproxy deproxy

    def setup() {

        properties = new TestProperties()
        this.reposePort = properties.reposePort
        this.targetPort = properties.targetPort
        this.url = properties.reposeEndpoint

        int dataStorePort = PortFinder.Singleton.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        // start a deproxy
        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())

    }

    @Unroll("start with good #componentLabel configs, should get #expectedResponseCode")
    def "start with good #componentLabel configs, should get #expectedResponseCode"() {

        given:
        // set the common and good configs
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/${componentLabel}-good", params)

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )
        repose.enableDebug()
        reposeLogSearch = new ReposeLogSearch(properties.getLogFile());
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)


        expect: "starting Repose with good configs should yield 200's"
        deproxy.makeRequest(url: url).receivedResponse.code == "${expectedResponseCode}"


        where:
        componentLabel            | expectedResponseCode
        "system-model"            | 200
        "container"               | 200
        "response-messaging"      | 200
        "rate-limiting"           | 200
        "versioning"              | 200
        "translation"             | 200
        "keystone-v2"             | 200
        "dist-datastore"          | 200
        "uri-identity"            | 200
        "header-identity"         | 200
        "ip-identity"             | 200
        "validator"               | 200
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

