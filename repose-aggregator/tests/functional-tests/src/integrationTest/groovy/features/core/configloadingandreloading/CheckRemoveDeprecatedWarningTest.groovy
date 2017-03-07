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

import framework.*
import org.rackspace.deproxy.Deproxy
import spock.lang.Specification

/**
 * Created by jennyvo on 7/15/14.
 */
class CheckRemoveDeprecatedWarningTest extends Specification {

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

        reposeLogSearch = new ReposeLogSearch(properties.getLogFile())

        int dataStorePort = PortFinder.instance.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        deproxy = new Deproxy()
        deproxy.addEndpoint(this.targetPort)

        // setup config provider
        reposeConfigProvider = new ReposeConfigurationProvider(properties.getConfigDirectory(), properties.getConfigTemplates())
    }

    def "Start repose with basic config, re-apply new config check for DEPRECATED msg"() {
        given:
        // set the common and good configs
        reposeLogSearch.cleanLog()
        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/configloadingandreloading/container-common", params)

        // start repose
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort
        )

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(url)


        expect: "starting Repose with good configs should yield 200"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"

        when: "apply config and wait for repose apply change"
        reposeConfigProvider.applyConfigs(
                "features/core/configloadingandreloading/container-reconfig", params)
        sleep 15000

        then: "Repose should still return good and DEPRECATED msg not appear"
        deproxy.makeRequest(url: url).receivedResponse.code == "200"
        reposeLogSearch.searchByString("DEPRECATED").size() == 0
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
