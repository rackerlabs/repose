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
class UriStripperLinkResourceJsonTokenIndexTest extends ReposeValveTest {

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
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/json/tokenindex", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        jsonBuilder = new JsonBuilder()
    }

    @Unroll
    def "when configured to update the token-index #index, the JSON response body link should be updated from #responseBodyLink to #modifiedResponseBodyLink"() {
        given: "the JSON response body has the configured link"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            "$jsonPath" responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        responseJson."$jsonPath" == modifiedResponseBodyLink

        where:
        jsonPath | index | responseBodyLink   | modifiedResponseBodyLink
        "link-a" | 0     | "/"                | "/$tenantId"
        "link-a" | 0     | "/a/b/c/d/e/f/g/h" | "/$tenantId/a/b/c/d/e/f/g/h"
        "link-b" | 1     | "/a/b/c/d/e/f/g/h" | "/a/$tenantId/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/f/g/h" | "/a/b/c/d/e/$tenantId/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e"       | "/a/b/c/d/e/$tenantId"
        "link-c" | 5     | "/a/b/c"           | "/a/b/c"
    }

    def "when a JSON response body contains multiple link-resources, they are all updated"() {
        given: "the JSON response body contains multiple link-resources"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        jsonBuilder {
            "link-a" responseBodyLink
            "link-b" responseBodyLink
            "link-c" responseBodyLink
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "all of the links in the response body are updated correctly"
        responseJson."link-a" == "/$tenantId/a/b/c/d/e/f/g/h"
        responseJson."link-b" == "/a/$tenantId/b/c/d/e/f/g/h"
        responseJson."link-c" == "/a/b/c/d/e/$tenantId/f/g/h"
    }

    def "when a complicated JSON path is configured, it should work fine"() {
        given: "the JSON response body contains the complicated JSON path link"
        def requestUrl = "/foo/$tenantId/bar"
        def responseBodyLink = "/foo/bar"
        jsonBuilder.foo {
            bar {
                baz {
                    link responseBodyLink
                }
            }
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, jsonBuilder.toString()) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def responseJson = jsonSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        responseJson.foo.bar.baz.link == requestUrl
    }
}
