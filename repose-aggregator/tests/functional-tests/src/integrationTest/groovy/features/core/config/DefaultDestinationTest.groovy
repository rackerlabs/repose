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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.PortFinder
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Slow
import spock.lang.Unroll

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

@Category(Slow)
class DefaultDestinationTest extends ReposeValveTest {

    Map params = [:]

    String errorMessage = "There should be one and only one default destination."

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

    @Unroll
    def "Fails to connect when defaults: #default1, #default2, #default3"() {
        given:
        // set the common and good configs
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1": default1, "default2": default2, "default3": default3
        ]
        repose.configurationProvider.applyConfigs("features/core/config/default-dest", params)
        reposeLogSearch.cleanLog()

        // start repose
        when: "starting Repose with more or less than one default destination endpoint"
        repose.start([waitOnJmxAfterStarting: false])

        then: "error should be logged"
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid default destination endpoint settings"
        deproxy.makeRequest(url: reposeEndpoint)
        then: "connection exception should be returned"
        thrown(ConnectException)


        where:
        default1 | default2 | default3
        false    | false    | false
        true     | true     | true
        true     | true     | false
        true     | false    | true
        false    | true     | true

    }

    @Unroll
    def "starts and returns 200 when defaults: #default1, #default2, #default3"() {
        given:
        // set the common and good configs
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1": default1, "default2": default2, "default3": default3
        ]
        repose.configurationProvider.applyConfigs("features/core/config/default-dest", params)

        // start repose
        reposeLogSearch.cleanLog()
        repose.start(killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)
        repose.waitForNon500FromUrl(reposeEndpoint)

        expect: "starting Repose with good configs should yield 200"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"

        where:
        default1 | default2 | default3
        true     | false    | false
        false    | true     | false
        false    | false    | true

    }

    @Unroll
    def "when defaults: #default1, #default2, #default3"() {
        given:
        // set the common and good configs
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)

        params += [
                "default1": defaultParamWrapper(default1),
                "default2": defaultParamWrapper(default2),
                "default3": defaultParamWrapper(default3)
        ]
        repose.configurationProvider.applyConfigs("features/core/config/default-dest-null", params)

        // start repose


        when: "starting Repose with more or less than one default destination endpoint"
        reposeLogSearch.cleanLog()
        repose.start([waitOnJmxAfterStarting: false])

        then: "error should be logged"
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "making a request to repose with and invalid default destination endpoint settings"
        deproxy.makeRequest(url: reposeEndpoint)
        then: "connection exception should be returned"
        thrown(ConnectException)


        where:
        default1 | default2 | default3
        null     | null     | null
        null     | null     | false
        null     | false    | false
        null     | true     | true
        null     | false    | null
        true     | null     | true
        true     | true     | null
        false    | null     | null
        false    | null     | false
        false    | false    | null
    }

    private def defaultParamWrapper(Object value) {
        return value == null ? '' : '\" default=\"' + value
    }
}
