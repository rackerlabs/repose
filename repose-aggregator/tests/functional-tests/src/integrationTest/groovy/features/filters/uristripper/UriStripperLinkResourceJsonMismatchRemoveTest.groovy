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
import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import scaffold.category.Filters

@Category(Filters)
class UriStripperLinkResourceJsonMismatchRemoveTest extends ReposeValveTest {

    def static String tenantId = "94828347"
    def static jsonSlurper = new JsonSlurper()
    def static responseHeaders = ["Content-Type": MimeType.APPLICATION_JSON.toString()]
    def jsonBuilder

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/json/mismatchremove", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        jsonBuilder = new JsonBuilder()
    }

    def "when configured to remove on mismatch, the response body link is removed if it can't be updated due to no recognizable tokens"() {
        given: "the link in the JSON response doesn't contain the previous nor following token"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        jsonBuilder {
            link responseBodyLink
            "other-field" "some value"
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        !responseJson.link  // link should have been removed
        responseJson."other-field" == "some value"  // unrelated field should remain unaltered
    }

    def "when configured to remove on mismatch, the response body alt-link is removed if the token index is too high for the link"() {
        given: "the alt-link in the JSON response doesn't contain enough tokens and the link is legit"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c"
        jsonBuilder {
            "alt-link" responseBodyLink
            "other-field" "some value"
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        !responseJson."alt-link"  // link should have been removed
        responseJson."other-field" == "some value"  // unrelated field should remain unaltered
    }

    def "when configured to remove on mismatch, the response body is not modified if the JSON path to the link does not resolve"() {
        given: "the JSON response doesn't contain the link field at all"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            "not-the-link" "/foo/bar"
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response body is not modified"
        mc.receivedResponse.body as String == jsonBuilder.toString()
    }
}
