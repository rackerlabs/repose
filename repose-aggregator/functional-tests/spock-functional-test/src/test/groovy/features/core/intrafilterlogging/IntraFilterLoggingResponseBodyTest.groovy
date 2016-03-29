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

package features.core.intrafilterlogging

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by mari9064 on 3/29/16.
 * Verifies that the response body sent from the origin service makes it to the client and is logged by the IntraFilter
 * Logger.  Also verifies that the request body from the client makes it to the origin service.
 */
class IntraFilterLoggingResponseBodyTest extends ReposeValveTest {
    def static String content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac " +
            "malesuada. Integer nec est turpis duis."
    def static logPreString = '"currentFilter":"'
    def static logPostStringRequest = /".*"requestBody":"$content"/
    def static logPostStringResponse = /","httpResponseCode":"200","responseBody":"$content"/
    def static configuredFilters = ["derp", "herp", "api-validator", "simple-rbac", "rate-limiting", "compression",
                                    "uri-stripper", "uri-normalization", "uri-user", "slf4j-http-logging", "merge-header",
                                    "header-translation", "header-normalization", "header-user", "content-type-stripper",
                                    "ip-user", "add-header"]

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/core/intrafilterlogging", params);
        repose.configurationProvider.applyConfigs("features/core/intrafilterlogging/responsebody", params);

        repose.start()

        deproxy.addEndpoint(properties.targetPort, 'origin service')

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "verify client gets the response body from the origin and that it is properly logged"() {
        given:
        def headers = ["x-roles": "raxRolesDisabled"]

        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/test",
                method: 'GET',
                headers: headers,
                defaultHandler: { new Response(200, "OK", [], content) })

        then:
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == 0
        mc.receivedResponse.body == content
        configuredFilters.each { filterName ->
            assert reposeLogSearch.searchByString(logPreString + filterName + logPostStringResponse).size() > 0
        }
    }

    def "verify origin receives request body from client and that it is properly logged"() {
        given:
        def headers = ["x-roles": "raxRolesDisabled"]

        when:
        MessageChain mc = deproxy.makeRequest(
                url: reposeEndpoint + "/test",
                method: 'POST',
                headers: headers,
                requestBody: content)

        then:
        mc.receivedResponse.code == "200"
        mc.orphanedHandlings.size() == 0
        mc.sentRequest.body == content
        configuredFilters.each { filterName ->
            assert reposeLogSearch.searchByString(logPreString + filterName + logPostStringRequest).size() > 0
        }
    }
}
