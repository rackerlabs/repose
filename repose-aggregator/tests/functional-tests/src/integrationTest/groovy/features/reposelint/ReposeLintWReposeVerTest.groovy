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

/**
 * Created by jennyvo on 4/7/16.
 *  addition test for check repose version "7.1.4.0"
 */
class ReposeLintWReposeVerTest extends Specification {
    @Shared
    ReposeLintLauncher reposeLintLauncher
    @Shared
    TestProperties testProperties
    @Shared
    ReposeLogSearch reposeLogSearch
    @Shared
    ReposeConfigurationProvider reposeConfigurationProvider
    def static configrole = "foyer"
    def static reposeVer = "7.1.4.0"
    static String allowedWithoutAuthorizationDesc = "Users with the '${configrole}' Identity role WILL pass through this component BUT authorization checks will not be performed"
    static String allowedWithAuthorizationDesc = "Users with the '${configrole}' Identity role WILL pass through this component IF AND ONLY IF their Identity service catalog contains an endpoint required by the authorization component"
    static String allowedDesc = "Users with the '${configrole}' Identity role WILL pass through this component"
    static String notAllowedDesc = "Users with the '${configrole}' Identity role WILL NOT pass through this component"

    def setupSpec() {
        this.testProperties = new TestProperties(this.getClass().canonicalName.replace('.', '/'))
        this.testProperties.userRole = configrole
        this.testProperties.reposeVersion = reposeVer
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

    @Unroll ("test with config: #configdir & #status")
    def "test with indivisual config"() {
        given:
        def params = testProperties.getDefaultTemplateParams()
        reposeConfigurationProvider.cleanConfigDirectory()
        reposeConfigurationProvider.applyConfigs(configdir, params)
        def roleAsIgnoreTenant
        if (checktype == "keystoneV2Check") {
            roleAsIgnoreTenant = "${configrole}AsPreAuthorized"
        } else if (checktype == "keystoneV3Check") {
            roleAsIgnoreTenant = "${configrole}AsBypassTenant"
        } else {
            roleAsIgnoreTenant = "${configrole}AsIgnoreTenant"
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
        jsonlog["${configrole}Status"] == status
        jsonlog["${configrole}StatusDescription"] == desc
        jsonlog.clusters.clusterId.get(0) == "repose"
        jsonlog.clusters[checktype][0]["filterName"] == filtername
        jsonlog.clusters[checktype][0]["filters"].size() != 0
        jsonlog.clusters[checktype][0]["filters"][0]["missingConfiguration"] == false
        jsonlog.clusters[checktype][0]["filters"][0][roleAsIgnoreTenant] == roleignore
        jsonlog.clusters[checktype][0]["filters"][0]["${configrole}Status"] == status
        jsonlog.clusters[checktype][0]["filters"][0]["${configrole}StatusDescription"] == desc
        (checktenantedmode == "no") || (jsonlog.clusters[checktype][0]["filters"][0]["inTenantedMode"] == tenantmode)

        where:
        configdir                                             | checktype         | filtername             | checktenantedmode | tenantmode | roleignore | status                        | desc
        "features/reposelint/keystonev2/tenanted"             | "keystoneV2Check" | "keystone-v2"          | "yes"             | true       | false      | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/tenanted/wfoyerrole"  | "keystoneV2Check" | "keystone-v2"          | "yes"             | true       | true       | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/tenanted/wconfigrole" | "keystoneV2Check" | "keystone-v2"          | "yes"             | true       | false      | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/authz"                | "keystoneV2Check" | "keystone-v2"          | "no"              | false      | false      | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/authzwconfigrole"     | "keystoneV2Check" | "keystone-v2"          | "no"              | false      | false      | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/keystonev2/authzwfoyerrole"      | "keystoneV2Check" | "keystone-v2"          | "no"              | false      | true       | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/clientauthn"                     | "authNCheck"      | "client-auth"          | "yes"             | false      | false      | "Allowed"                     | allowedDesc
        "features/reposelint/clientauthn/tenanted"            | "authNCheck"      | "client-auth"          | "yes"             | true       | false      | "NotAllowed"                  | notAllowedDesc
        "features/reposelint/clientauthn/tenantedwfoyerrole"  | "authNCheck"      | "client-auth"          | "yes"             | true       | true       | "Allowed"                     | allowedDesc
        "features/reposelint/clientauthz"                     | "authZCheck"      | "client-authorization" | "no"              | false      | false      | "AllowedWithAuthorization"    | allowedWithAuthorizationDesc
        "features/reposelint/clientauthz/wfoyerrole"          | "authZCheck"      | "client-authorization" | "no"              | false      | true       | "AllowedWithoutAuthorization" | allowedWithoutAuthorizationDesc
    }
}
