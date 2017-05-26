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

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeConfigurationProvider
import org.openrepose.framework.test.ReposeLintLauncher
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.TestProperties
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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

    static String allowedWithoutAuthorizationDesc = "Users with the 'foyer' Identity role WILL pass through this component BUT authorization checks will not be performed"
    static String allowedWithAuthorizationDesc = "Users with the 'foyer' Identity role WILL pass through this component IF AND ONLY IF their Identity service catalog contains an endpoint required by the authorization component"
    static String notAllowedDesc = "Users with the 'foyer' Identity role WILL NOT pass through this component"

    def setupSpec() {
        this.testProperties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))

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
        //top level status
        jsonlog["foyerStatus"] == "NotAllowed"
        jsonlog["foyerStatusDescription"] == notAllowedDesc

        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters["authNCheck"][0]["filterName"] == "client-auth"
        jsonlog.clusters["authNCheck"][0]["filters"].size() != 0
        jsonlog.clusters["authNCheck"][0]["filters"][0]["missingConfiguration"] == true
        jsonlog.clusters["authNCheck"][0]["filters"][0]["foyerStatus"] == "NotAllowed"
        jsonlog.clusters["authNCheck"][0]["filters"][0]["foyerStatusDescription"] == notAllowedDesc
    }

    @Unroll("test with config: #configdir & #status")
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
        //top level status
        jsonlog["foyerStatus"] == status
        jsonlog["foyerStatusDescription"] == desc
        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters[checktype][0]["filterName"] == filtername
        jsonlog.clusters[checktype][0]["filters"].size() != 0
        jsonlog.clusters[checktype][0]["filters"][0]["missingConfiguration"] == false
        jsonlog.clusters[checktype][0]["filters"][0][foyerAsIgnoreTenant] == foyerignore
        jsonlog.clusters[checktype][0]["filters"][0]["foyerStatus"] == status
        jsonlog.clusters[checktype][0]["filters"][0]["foyerStatusDescription"] == desc
        jsonlog.clusters[checktype][0]["filters"][0]["inTenantedMode"] == tenantmode

        where:
        configdir                                            | checktype         | filtername    | tenantmode | foyerignore | status                        | desc
        "features/reposelint/keystonev2"                     | "keystoneV2Check" | "keystone-v2" | false      | false       | "AllowedWithoutAuthorization" | allowedWithoutAuthorizationDesc
        "features/reposelint/keystonev2/tenanted"            | "keystoneV2Check" | "keystone-v2" | true       | false       | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/tenanted/wfoyerrole" | "keystoneV2Check" | "keystone-v2" | true       | true        | "AllowedWithoutAuthorization" | allowedWithoutAuthorizationDesc
        "features/reposelint/keystonev2/authz"               | "keystoneV2Check" | "keystone-v2" | false      | false       | "AllowedWithAuthorization"    | allowedWithAuthorizationDesc
        "features/reposelint/keystonev2/authzwfoyerrole"     | "keystoneV2Check" | "keystone-v2" | false      | true        | "AllowedWithoutAuthorization" | allowedWithoutAuthorizationDesc
    }
}
