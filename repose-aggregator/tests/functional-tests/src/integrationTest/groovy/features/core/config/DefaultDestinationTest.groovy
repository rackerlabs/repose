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
import scaffold.category.Core
import scaffold.category.Slow
import spock.lang.Unroll

import static org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils.waitForCondition
import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

@Category(Core)
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
            datastorePort: dataStorePort,
        ]

        reposeLogSearch.cleanLog()
        repose.configurationProvider.cleanConfigDirectory()
    }

    def cleanup() {
        repose?.stop()
    }

    @Unroll("not exactly one default destination with defaults: #default1, #default2, #default3")
    def "when there is not exactly one default destination, an error is recorded and a request cannot be processed"() {
        given: "default destination parameters to apply to configuration templates"
        params += [
            default1: defaultParamWrapper(default1),
            default2: defaultParamWrapper(default2),
            default3: defaultParamWrapper(default3)
        ]

        and: "Repose configuration"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)
        repose.configurationProvider.applyConfigs("features/core/config/default-dest", params)

        when: "Repose is started"
        repose.start(waitOnJmxAfterStarting: false)

        then: "error should be logged"
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            new File(reposeLogSearch.logFileLocation).exists()
        }
        waitForCondition(repose.clock, "${MAX_STARTUP_TIME}s", "2s") {
            reposeLogSearch.searchByString(errorMessage).size() != 0
        }

        when: "a request is made to Repose"
        deproxy.makeRequest(url: reposeEndpoint)

        then: "Deproxy should fail to connect"
        thrown(ConnectException)

        where:
        default1 | default2 | default3
        false    | false    | false
        true     | true     | true
        true     | true     | false
        true     | false    | true
        false    | true     | true
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

    @Unroll("exactly one default destination with defaults: #default1, #default2, #default3")
    def "when there is exactly one default destination, a request can be successfully processed"() {
        given: "default destination parameters to apply to configuration templates"
        params += [
            default1: defaultParamWrapper(default1),
            default2: defaultParamWrapper(default2),
            default3: defaultParamWrapper(default3)
        ]

        and: "Repose configuration"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/common", params)
        repose.configurationProvider.applyConfigs("features/core/config/default-dest", params)

        when: "Repose is started"
        repose.start()

        then: "a request is processed and a 200 is returned"
        deproxy.makeRequest(url: reposeEndpoint).receivedResponse.code == "200"

        where:
        default1 | default2 | default3
        true     | false    | false
        true     | null     | false
        true     | false    | null
        true     | null     | null
        false    | true     | false
        null     | true     | false
        false    | true     | null
        null     | true     | null
        false    | false    | true
        null     | false    | true
        false    | null     | true
        null     | null     | true
    }

    private static def defaultParamWrapper(Object value) {
        return value == null ? '' : "default=\"$value\""
    }
}
