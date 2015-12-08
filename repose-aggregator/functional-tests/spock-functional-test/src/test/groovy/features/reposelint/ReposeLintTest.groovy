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
package features.reposelint

import framework.ReposeConfigurationProvider
import framework.ReposeLintLauncher
import framework.ReposeLogSearch
import framework.TestProperties

class ReposeLintTest {
    ReposeLintLauncher reposeLintLauncher
    TestProperties testProperties
    ReposeLogSearch reposeLogSearch
    ReposeConfigurationProvider reposeConfigurationProvider

    def setupSpec() {
        this.testProperties = new TestProperties()

        this.reposeConfigurationProvider = new ReposeConfigurationProvider(testProperties)

        this.reposeLintLauncher = new ReposeLintLauncher(reposeConfigurationProvider, testProperties)
        this.reposeLintLauncher.enableDebug()

        this.reposeLogSearch = new ReposeLogSearch(testProperties.getReposeLintLogFile())
    }

    def setup() {
        reposeLogSearch.cleanLog()
    }

    def cleanup() {
        reposeLintLauncher.stop()
    }

    // todo
    def "some test"() {
        given:
        def params = testProperties.getDefaultTemplateParams()
        reposeConfigurationProvider.cleanConfigDirectory()
        reposeConfigurationProvider.applyConfigs("some/config/dir", params)

        when:
        reposeLintLauncher.start("verify-try-it-now")

        then:
        reposeLogSearch.searchByString("some output")
    }
}