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
package features.filters.uristripper

import groovy.json.JsonBuilder
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import scaffold.category.Filters

import javax.servlet.http.HttpServletResponse

@Category(Filters)
class UriStripperRequestLinkResourceJsonMismatchFailTest extends ReposeValveTest {

    def static String tenantId = "94828347"
    def static requestHeaders = ["Content-Type": MimeType.APPLICATION_JSON.toString()]
    def jsonBuilder

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/request/json/mismatchfail", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        jsonBuilder = new JsonBuilder()
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the token index is too high for the link"() {
        given: "the alt-link in the JSON request doesn't contain enough tokens and the link is legit"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            link "/foo/bar"
            "alt-link" "/a/b/c"
            "other-field" "some value"
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: jsonBuilder.toString())

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_BAD_REQUEST
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the JSON path to the link does not resolve"() {
        given: "the JSON request doesn't contain the link field at all"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            "not-the-link" "/foo/bar"
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: jsonBuilder.toString())

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_BAD_REQUEST
    }
}
