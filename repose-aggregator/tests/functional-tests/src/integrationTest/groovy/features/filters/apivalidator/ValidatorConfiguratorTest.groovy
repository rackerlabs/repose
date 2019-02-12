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
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeLogSearch
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing

import javax.servlet.http.HttpServletResponse

@Category(XmlParsing)
class ValidatorConfiguratorTest extends ReposeValveTest {
    def static params = [:]

    def setupSpec() {
        params = properties.getDefaultTemplateParams()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def setup() {
        cleanLogDirectory()
        reposeLogSearch = new ReposeLogSearch(logFile)
    }

    def cleanup() {
        if (repose) {
            repose.stop()
        }
    }

    def errorMessage = "WADL Processing Error:"

    def "when loading validators on startup, should work with local uri path"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path generic_pass.wadl"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/wadlpath/good", params)
        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "GET", headers: ['X-Roles': 'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "request returns a 404 and and no error wadl error is thrown"
        wadlError.size() == 0
        resp.getReceivedResponse().code as Integer == HttpServletResponse.SC_NOT_FOUND
    }

    def "when loading validators on startup, should fail bad local uri path"() {

        given: "repose is started using a non-uri path for the wadl, in this case the path does_not_exist.wadl"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/wadlpath/bad", params)
        repose.start([waitOnJmxAfterStarting: false])
        repose.waitForNon500FromUrl(reposeEndpoint)

        when: "a request is made using the api validator"
        def resp = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "GET", headers: ['X-Roles': 'test_user']])
        def List<String> wadlError;
        wadlError = reposeLogSearch.searchByString(errorMessage)

        then: "wadl error is thrown"
        wadlError.size() == 2
    }

    def "Verify dot output file not unique for each validator"() {
        given: "config"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/badconfig/dotoutputnotunique", params)

        when: "repose start"
        repose.start()
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "GET", headers: ['x-roles': 'default']])

        then:
        reposeLogSearch.searchByString("Assertion failed for schema type 'ValidatorConfiguration'. Dot output files must be unique").size() > 0
        mc.receivedResponse.code == "503"
    }

    def "Verify validator name not unique"() {
        given: "config"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/badconfig/validatornamenotunique", params)

        when: "repose start"
        repose.start()
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "GET", headers: ['x-roles': 'default']])

        then:
        reposeLogSearch.searchByString("Assertion failed for schema type 'ValidatorConfiguration'. Validator names must be unique.").size() > 0
        mc.receivedResponse.code == "503"
    }

    def "Verify only one default validator is defined"() {
        given: "config"
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/badconfig/morethanonetruedefined", params)

        when: "repose start"
        repose.start()
        MessageChain mc = deproxy.makeRequest([url: reposeEndpoint + "/test", method: "GET", headers: ['x-roles': 'default']])

        then:
        reposeLogSearch.searchByString("Assertion failed for schema type 'ValidatorConfiguration'. Only one default validator may be defined.").size() > 0
        mc.receivedResponse.code == "503"
    }
}
