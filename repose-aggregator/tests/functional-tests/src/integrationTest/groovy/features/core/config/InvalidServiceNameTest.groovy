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
package features.core.config

import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.openrepose.framework.test.ReposeLauncher.getMAX_STARTUP_TIME

class InvalidServiceNameTest extends ReposeValveTest {

    Map params = [:]
    String errorMessage = "cvc-enumeration-valid: Value 'not-a-service' is not facet-valid with respect to enumeration"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def setup() {

        int dataStorePort = PortFinder.instance.getNextOpenPort()
        params = properties.getDefaultTemplateParams()

        params += [
                'datastorePort': dataStorePort,
        ]

        repose.configurationProvider.cleanConfigDirectory()
    }

    def cleanup() {
        repose?.stop()
    }

    def "start with invalid service name in system model configs, should log error and fail to connect"() {
        given:
        // set the common and good configs
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)
        repose.configurationProvider.applyConfigs("features/core/config/service-name-bad", params)

        // start repose
        when: "starting Repose with an invalid service name"
        reposeLogSearch.cleanLog()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        then: "error should be logged"
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid service name"
        deproxy.makeRequest(url: reposeEndpoint)
        then: "connection exception should be returned"
        thrown(ConnectException)

    }


    def "start with valid service name in system model configs, change to bad, should log the exception and get 200"() {
        given:
        // set the common and good configs
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)
        repose.configurationProvider.applyConfigs("features/core/config/service-name-good", params)

        // start repose
        reposeLogSearch.cleanLog()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)

        expect: "starting Repose with good configs should yield 200"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"


        when: "the configs are changed to have an invalid service and we wait for Repose to pick up the change"
        repose.configurationProvider.applyConfigs("features/core/config/service-name-bad", params)

        then: "error should be logged and Repose should still return 200"
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"

    }
}

