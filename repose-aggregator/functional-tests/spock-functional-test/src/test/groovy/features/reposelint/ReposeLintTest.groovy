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
import static org.junit.Assert.*;
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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
    @Unroll("test with config: #configdir")
    def "some test"() {
        given:
        def params = testProperties.getDefaultTemplateParams()
        reposeConfigurationProvider.cleanConfigDirectory()
        reposeConfigurationProvider.applyConfigs(configdir, params)
        def reposever = reposeLintLauncher.reposeVer

        when:
        reposeLintLauncher.start("verify-try-it-now")
        def debugport = reposeLintLauncher.debugPort
        def log = reposeLogSearch.logToString() - "Listening for transport dt_socket at address: 10014"
        println log
        def slurper = new JsonSlurper()
        def jsonlog = slurper.parseText(log)

        then:
        reposeLogSearch.searchByString(debugport.toString())
        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters[checktype][0]["filterName"] == filtername
        jsonlog.clusters[checktype][0]["filters"].size() != 0
        //jsonlog.clusters[checktype][0]["filters"][0]["inTenantedMode"] == tenantmode
        jsonlog.clusters[checktype][0]["filters"][0]["foyerStatus"] == status
        if (checktenantedmode == "yes") {
            assertTrue(jsonlog.clusters[checktype][0]["filters"][0]["inTenantedMode"] == tenantmode)
        }



        where:
        configdir                                  | checktype         | filtername             | checktenantedmode | tenantmode | status
        "features/reposelint/clientauthn"          | "authNCheck"      | "client-auth"          | "yes"             | false      | "Allowed"
        "features/reposelint/clientauthn/tenanted" | "authNCheck"      | "client-auth"          | "yes"             | true       | "NotAllowed"
        "features/reposelint/clientauthz"          | "authZCheck"      | "client-authorization" | "no"              | false      | "NotAllowed"
        "features/reposelint/keystonev2"           | "keystoneV2Check" | "client-authorization" | "yes"             | false      | "Allowed"
        "features/reposelint/keystonev2/tenanted"  | "keystoneV2Check" | "client-authorization" | "yes"             | true       | "NotAllowed"
        "features/reposelint/keystonev2/authz"     | "keystoneV2Check" | "client-authorization" | "no"              | false      | "NotAllowed"
    }

}