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
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Core
import spock.lang.Shared

import java.util.concurrent.TimeUnit

@Category(Core)
class EnvironmentVariableFailTest extends ReposeValveTest {

    @Shared
    def ENV_KEY = "Howdy"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(port: properties.targetPort, name: "Origin Service")

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/config/env-vars", params)
        repose.addToEnvironment("ENV_KEY", ENV_KEY)
        repose.start()
    }

    def "Repose should log if a required environment variable is undefined"() {
        when: "a repose environment isn't fully configured,"
        then: "it should be logged."
        def logSearch = reposeLogSearch.awaitByString(
            "Variable 'ENV_VAL' undefined",
            1,
            10,
            TimeUnit.SECONDS
        )
        !logSearch.empty
    }
}
