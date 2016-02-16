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
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertTrue

/**
 * Update on 01/28/15
 *  - replace client-auth with keystone-v2
 */

class ReposeLintTest extends Specification {
    @Shared
    ReposeLintLauncher reposeLintLauncher
    @Shared
    TestProperties testProperties
    @Shared
    ReposeLogSearch reposeLogSearch
    @Shared
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
    def "Test missing config"() {
        given:
        def params = testProperties.getDefaultTemplateParams()
        reposeConfigurationProvider.cleanConfigDirectory()
        reposeConfigurationProvider.applyConfigs("features/reposelint/missingconfig", params)

        when:
        reposeLintLauncher.start("verify-try-it-now")
        def debugport = reposeLintLauncher.debugPort
        def log = reposeLogSearch.logToString() - ("Listening for transport dt_socket at address: " + debugport)
        println log
        def slurper = new JsonSlurper()
        def jsonlog = slurper.parseText(log)

        then:
        reposeLogSearch.searchByString(debugport.toString())
        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters["authNCheck"][0]["filterName"] == "client-auth"
        jsonlog.clusters["authNCheck"][0]["filters"].size() != 0
        jsonlog.clusters["authNCheck"][0]["filters"][0]["missingConfiguration"] == true
        jsonlog.clusters["authNCheck"][0]["filters"][0]["foyerStatus"] == "NotAllowed"
    }

    @Unroll("test with config: #configdir")
    def "test individual components"() {
        given:
        def params = testProperties.getDefaultTemplateParams()
        reposeConfigurationProvider.cleanConfigDirectory()
        reposeConfigurationProvider.applyConfigs(configdir, params)
        def foyerAsIgnoreTenant
        if (checktype == "keystoneV2Check") {
            foyerAsIgnoreTenant = "foyerAsPreAuthorized"
        } else if (checktype == "keystoneV3Check") {
            foyerAsIgnoreTenant = "foyerAsBypassTenant"
        } else {
            foyerAsIgnoreTenant = "foyerAsIgnoreTenant"
        }

        when:
        reposeLintLauncher.start("verify-try-it-now")
        def debugport = reposeLintLauncher.debugPort
        def log = reposeLogSearch.logToString() - ("Listening for transport dt_socket at address: " + debugport)
        println log
        def slurper = new JsonSlurper()
        def jsonlog = slurper.parseText(log)

        then:
        reposeLogSearch.searchByString(debugport.toString())
        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters[checktype][0]["filterName"] == filtername
        jsonlog.clusters[checktype][0]["filters"].size() != 0
        jsonlog.clusters[checktype][0]["filters"][0]["missingConfiguration"] == false
        jsonlog.clusters[checktype][0]["filters"][0][foyerAsIgnoreTenant] == foyerignore
        jsonlog.clusters[checktype][0]["filters"][0]["foyerStatus"] == status
        if (checktenantedmode == "yes") {
            assertTrue(jsonlog.clusters[checktype][0]["filters"][0]["inTenantedMode"] == tenantmode)
        }

        where:
        configdir                                            | checktype         | filtername    | checktenantedmode | tenantmode | foyerignore | status
        "features/reposelint/keystonev2"                     | "keystoneV2Check" | "keystone-v2" | "yes"             | false      | false       | "AllowedNotAuthorized"
        "features/reposelint/keystonev2/tenanted"            | "keystoneV2Check" | "keystone-v2" | "yes"             | true       | false       | "NotAllowed"
        "features/reposelint/keystonev2/tenanted/wfoyerrole" | "keystoneV2Check" | "keystone-v2" | "yes"             | true       | true        | "AllowedNotAuthorized"
        "features/reposelint/keystonev2/authz"               | "keystoneV2Check" | "keystone-v2" | "no"              | false      | false       | "NotAllowed"
        "features/reposelint/keystonev2/authzwfoyerrole"     | "keystoneV2Check" | "keystone-v2" | "no"              | false      | true        | "AllowedNotAuthorized"
    }
}