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

/**
 * Created by jennyvo on 7/15/14.
 */
class CheckRemoveDeprecatedWarningTest extends ReposeValveTest {

    Map params = [:]
    Deproxy deproxy

    def setup() {

        int dataStorePort = PortFinder.instance.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def "Start repose with basic config, re-apply new config check for DEPRECATED msg"() {
        given:
        // set the common and good configs
        reposeLogSearch.cleanLog()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/configloadingandreloading/container-common", params)

        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)


        expect: "starting Repose with good configs should yield 200"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"

        when: "apply config and wait for repose apply change"
        repose.configurationProvider.applyConfigs(
                "features/core/configloadingandreloading/container-reconfig", params)
        sleep 15000

        then: "Repose should still return good and DEPRECATED msg not appear"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"
        reposeLogSearch.searchByString("DEPRECATED").size() == 0
    }
}
