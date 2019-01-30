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

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing

@Category(XmlParsing)
class ApiValidatorDelegatingWAuditingTest extends ReposeValveTest {
    def static originEndpoint

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/delegable/withauditing", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def "delegating mode should not break intra-filter logging"() {
        when:
        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint + "/audit",
            method: method,
            headers: ['x-tRACE-rEQUEST': 'true'] + headers,
            requestBody: "horrible input")

        then:
        String logLine = reposeLogSearch.searchByString("TRACE intrafilter-logging - .*\"currentFilter\":\"derp\",\"httpMethod\":\"POST\"")
        logLine.length() > 0
        String jsonpart = logLine.substring(logLine.indexOf("{"))
        println(jsonpart)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(jsonpart)
        reposeLogSearch.searchByString("intrafilterlogging.RequestLog - Unable to populate request body").size() != 0
        reposeLogSearch.searchByString("java.io.IOException: Stream closed").size() != 0
        mc.receivedResponse.code == "400"
        mc.receivedResponse.message == "Bad Content: Content is not allowed in prolog."
        result.httpMethod == method
        result.headers["Host"] == reposeEndpoint - "http://"
        result.headers["User-Agent"].contains("deproxy")

        where:
        method | headers
        "POST" | ["x-roles": "default", "Content-Type": "application/atom+xml"]
    }
}
