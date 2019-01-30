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
import scaffold.category.Filters
import spock.lang.Unroll

@Category(Filters)
class UriStripperRequestLinkResourceJsonTokenIndexTest extends ReposeValveTest {

    def static String tenantId = "94828347"
    def static jsonSlurper = new JsonSlurper()
    def static requestHeaders = ["Content-Type": MimeType.APPLICATION_JSON.toString()]
    def jsonBuilder

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/request/json/tokenindex", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        jsonBuilder = new JsonBuilder()
    }

    @Unroll
    def "when configured to update the token-index #index, the JSON request body link should be updated from #requestBodyLink to #modifiedRequestBodyLink"() {
        given: "the JSON request body has the configured link"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            "$jsonPath" requestBodyLink
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: jsonBuilder.toString())

        and: "the received request JSON is parsed"
        def receivedRequestJson = jsonSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        receivedRequestJson."$jsonPath" == modifiedRequestBodyLink

        where:
        jsonPath | index | requestBodyLink              | modifiedRequestBodyLink
        "link-a" | 0     | "/$tenantId"                 | "/"
        "link-a" | 0     | "/$tenantId/a/b/c/d/e/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-b" | 1     | "/a/$tenantId/b/c/d/e/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/$tenantId/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/$tenantId"       | "/a/b/c/d/e"
        "link-c" | 5     | "/a/b/c"                     | "/a/b/c"
    }

    @Unroll
    def "when configured to update the token-index, the JSON request body link should NOT be updated when the HTTP method does not match"() {
        given: "the JSON request body has the configured link"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder {
            "$jsonPath" requestBodyLink
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
            method: "PUT",
            headers: requestHeaders,
            requestBody: jsonBuilder.toString())

        and: "the received request JSON is parsed"
        def receivedRequestJson = jsonSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        receivedRequestJson."$jsonPath" == requestBodyLink

        where:
        jsonPath | index | requestBodyLink
        "link-a" | 0     | "/$tenantId"
        "link-a" | 0     | "/$tenantId/a/b/c/d/e/f/g/h"
    }

    def "when a JSON request body contains multiple link-resources, they are all updated"() {
        given: "the JSON request body contains multiple link-resources"
        def requestUrl = "/foo/$tenantId/bar"
        def expectedBodyLink = "/a/b/c/d/e/f/g/h"
        jsonBuilder {
            "link-a" "/$tenantId/a/b/c/d/e/f/g/h"
            "link-b" "/a/$tenantId/b/c/d/e/f/g/h"
            "link-c" "/a/b/c/d/e/$tenantId/f/g/h"
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: jsonBuilder.toString())

        and: "the received request JSON is parsed"
        def receivedRequestJson = jsonSlurper.parseText(mc.handlings[0].request.body as String)

        then: "all of the links in the response body are updated correctly"
        receivedRequestJson."link-a" == expectedBodyLink
        receivedRequestJson."link-b" == expectedBodyLink
        receivedRequestJson."link-c" == expectedBodyLink
    }

    def "when a complicated JSON path is configured, it should work fine"() {
        given: "the JSON request body contains the complicated JSON path link"
        def requestUrl = "/foo/$tenantId/bar"
        jsonBuilder.foo {
            bar {
                baz {
                    link requestUrl
                }
            }
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: jsonBuilder.toString())

        and: "the received request JSON is parsed"
        def receivedRequestJson = jsonSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the response body link is modified"
        receivedRequestJson.foo.bar.baz.link == "/foo/bar"
    }
}
