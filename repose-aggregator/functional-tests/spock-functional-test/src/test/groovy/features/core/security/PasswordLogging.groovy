/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package features.core.security

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy

/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
class PasswordLogging extends ReposeValveTest {

    def setupSpec() {
        cleanLogDirectory()
        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/security/before", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def cleanupSpec() {
        deproxy.shutdown()
        repose.stop()
    }

    static def params

    def "identity passwords in auth configs are not logged in plaintext"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigs("features/core/security/after", params, /*sleepTime*/ 25)

        when: "I search for DEBUG logs for the configuration updated log entry"
        List<String> logs = reposeLogSearch.searchByString("Configuration Updated")

        then: "passwords in the DEBUG log are not logged in plaintext"
        logs.size() == 3
        logs[0].contains("password=*******") == true
        logs[1].contains("password=*******") == true
        logs[2].contains("password=*******") == true
    }


}
