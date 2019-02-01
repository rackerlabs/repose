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
import spock.lang.Unroll

@Category(Filters)
class UriStripperLinkResourceJsonTest extends ReposeValveTest {

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
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/json", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        jsonBuilder = new JsonBuilder()
    }

    def "when the uri does not match the configured uri-path-regex, the response body should not be modified"() {
        given: "url does not match configured uri-path-regex"
        def requestUrl = "/bar/$tenantId/path/to/resource"

        and: "a JSON response body contains a link without a tenantId"
        def responseBodyLink = "/bar/path/to/resource"
        jsonBuilder {
            link responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the link in the response body should remain unmodified"
        responseJson.link == responseBodyLink
    }

    @Unroll
    def "when the HTTP method #method is used, the response body link should be #expectedResponseBodyLink"() {
        given: "a JSON response body contains a link without a tenantId"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/foo/bar"
        jsonBuilder {
            link responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, method: method, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is the expected value for the given method"
        responseJson.link == expectedResponseBodyLink

        where:
        method    | expectedResponseBodyLink
        "GET"     | "/foo/$tenantId/bar"
        "DELETE"  | "/foo/bar"
        "POST"    | "/foo/$tenantId/bar"
        "PUT"     | "/foo/bar"
        "PATCH"   | "/foo/bar"
        "TRACE"   | "/foo/bar"
    }

    def "when the response is not JSON, the response body is not modified"() {
        given: "a non-JSON response body"
        def requestUrl = "/foo/$tenantId/bar"
        def body = "There's a million things I haven't done, just you wait"
        def responseHandler = { request ->
            new Response(200, null, ["Content-Type": MimeType.TEXT_PLAIN.toString()], body) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response body is not modified"
        mc.receivedResponse.body as String == body
    }

    @Unroll
    def "the response body link can be updated using the #position token only"() {
        given: "the link in the JSON response only contains one of the tokens"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            link responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is correctly updated"
        responseJson.link == modifiedResponseBodyLink

        where:
        position    | responseBodyLink | modifiedResponseBodyLink
        "previous"  | "/a/b/c/foo/baz" | "/a/b/c/foo/$tenantId/baz"
        "following" | "/jack/jill/bar" | "/jack/jill/$tenantId/bar"
    }

    def "when configured to continue on mismatch, the response body link is not modified if it can't be updated due to no recognizable tokens"() {
        given: "the link in the JSON response doesn't contain the previous nor following token"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        jsonBuilder {
            link responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        responseJson.link == responseBodyLink
    }

    def "when configured to continue on mismatch, the response body is not modified if the JSON path to the link does not resolve"() {
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
