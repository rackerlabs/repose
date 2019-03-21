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
package features.core.security

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core

import java.util.concurrent.TimeUnit

/**
 * D-15183 Ensure passwords are not logged when in DEBUG mode and config files are updated.
 */
@Category(Core)
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

    static def params

    def "identity passwords in auth configs are not logged in plaintext"() {

        given: "Repose configs are updated"
        repose.configurationProvider.applyConfigs("features/core/security/after", params)

        when: "I wait for the configuration to be updated"
        reposeLogSearch.awaitByString(
            "Configuration Updated: org.openrepose.filters.keystonev2.config.KeystoneV2AuthenticationConfig",
            1,
            25,
            TimeUnit.SECONDS
        )

        then: "passwords in the DEBUG log are not logged in plaintext"
        // Check the specific username/password in the keystone filter
        reposeLogSearch.searchByString("admin_username").size() == 0
        reposeLogSearch.searchByString("password-for-password-logging-test").size() == 0
    }


}
