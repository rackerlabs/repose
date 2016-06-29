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

import framework.ReposeValveTest
import groovy.xml.MarkupBuilder
import org.openrepose.commons.utils.http.media.MimeType
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

class UriStripperLinkResourceXmlTest extends ReposeValveTest {

    def static String tenantId = "138974928"
    def static xmlSlurper = new XmlSlurper()
    def static responseHeaders = ["Content-Type": MimeType.APPLICATION_XML.toString()]
    def stringWriter
    def xmlBuilder

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/xml", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        stringWriter = new StringWriter()
        xmlBuilder = new MarkupBuilder(stringWriter)
    }

    def simpleXmlBodyWithLink(String responseBodyLink) {
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title lang: 'en', 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                link responseBodyLink
            }
        }

        stringWriter.toString()
    }

    def "when the uri does not match the configured uri-path-regex, the response body should not be modified"() {
        given: "url does not match any configured uri-path-regex"
        def requestUrl = "/matches/nothing/$tenantId/some/more/resources"

        and: "an XML response body contains a link without a tenantId"
        def responseBodyLink = "/matches/nothing/some/more/resources"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the link in the response body should remain unmodified"
        bookstore.book.link == responseBodyLink
    }

    @Unroll
    def "when the HTTP method #method is used, the response body link should be #expectedResponseBodyLink"() {
        given: "an XML response body contains a link without a tenantId"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def responseBodyLink = "/continue/foo/bar"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, method: method, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is the expected value for the given method"
        bookstore.book.link == expectedResponseBodyLink

        where:
        method    | expectedResponseBodyLink
        "GET"     | "/continue/foo/$tenantId/bar"
        "DELETE"  | "/continue/foo/bar"
        "POST"    | "/continue/foo/$tenantId/bar"
        "PUT"     | "/continue/foo/bar"
        "PATCH"   | "/continue/foo/bar"
        "TRACE"   | "/continue/foo/bar"
    }

    @Unroll
    def "when the response is #contentType, the response body is not modified"() {
        given: "a plain text response body"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def responseHandler = { request -> new Response(200, null, ["Content-Type": contentType.toString()], body) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response body is not modified"
        mc.receivedResponse.body as String == body

        where:
        contentType               | body
        MimeType.TEXT_PLAIN       | "There's a million things I haven't done, just you wait"
        MimeType.APPLICATION_JSON | """{"name": "Requested donation","price": 3.50,"tags": ["Loch Ness Monster", "tree fiddy"]}"""
    }

    @Unroll
    def "the response body link can be updated using the #position token only"() {
        given: "the link in the XML response only contains one of the tokens"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is correctly updated"
        bookstore.book.link == modifiedResponseBodyLink

        where:
        position    | responseBodyLink | modifiedResponseBodyLink
        "previous"  | "/a/b/c/foo/baz" | "/a/b/c/foo/$tenantId/baz"
        "following" | "/jack/jill/bar" | "/jack/jill/$tenantId/bar"
    }

    def "when configured to continue on mismatch, the response body link is not modified if it can't be updated due to no recognizable tokens"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        bookstore.book.link == responseBodyLink
    }

    def "when configured to continue on mismatch, the response body is not modified if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def body = "<bookstore/>"
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response body is not modified"
        mc.receivedResponse.body as String == body
    }

    @Unroll
    def "when configured to update the token-index #index, the XML response body link should be updated from #responseBodyLink to #modifiedResponseBodyLink"() {
        given: "the XML response body has the configured link"
        def requestUrl = "/continue-index/foo/$tenantId/bar"
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                "$xPath" responseBodyLink
            }
        }
        def body = stringWriter.toString()
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        bookstore.book."$xPath" == modifiedResponseBodyLink

        where:
        xPath    | index | responseBodyLink   | modifiedResponseBodyLink
        "link-a" | 0     | "/"                | "/$tenantId"
        "link-a" | 0     | "/a/b/c/d/e/f/g/h" | "/$tenantId/a/b/c/d/e/f/g/h"
        "link-b" | 1     | "/a/b/c/d/e/f/g/h" | "/a/$tenantId/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/f/g/h" | "/a/b/c/d/e/$tenantId/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e"       | "/a/b/c/d/e/$tenantId"
        "link-c" | 5     | "/a/b/c"           | "/a/b/c"
    }

    def "when an XML response body contains multiple link-resources, they are all updated"() {
        given: "the XML response body contains multiple link-resources"
        def requestUrl = "/continue-index/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                "link-a" responseBodyLink
                "link-b" responseBodyLink
                "link-c" responseBodyLink
            }
        }
        def body = stringWriter.toString()
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "all of the links in the response body are updated correctly"
        bookstore.book."link-a" == "/$tenantId/a/b/c/d/e/f/g/h"
        bookstore.book."link-b" == "/a/$tenantId/b/c/d/e/f/g/h"
        bookstore.book."link-c" == "/a/b/c/d/e/$tenantId/f/g/h"
    }

    def "when configured to remove on mismatch, the response body link is removed if it can't be updated due to no recognizable tokens"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/remove/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        !bookstore.book.link  // link should have been removed
        bookstore.book.author == "Some person"  // unrelated field should remain unaltered
    }

    def "when configured to remove on mismatch, the response body is not modified if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/remove/foo/$tenantId/bar"
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
            }
        }
        def body = stringWriter.toString()
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response body is not modified"
        mc.receivedResponse.body as String == body
    }

    def "when configured to remove on mismatch, the response body link is removed if the token index is too high for the link"() {
        given: "the link in the XML response doesn't contain enough tokens"
        def requestUrl = "/remove-index/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        !bookstore.book.link  // link should have been removed
        bookstore.book.author == "Some person"  // unrelated field should remain unaltered
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the response body link can't be updated due to no recognizable tokens"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/fail/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR as String
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/fail/foo/$tenantId/bar"
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
            }
        }
        def body = stringWriter.toString()
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR as String
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the token index is too high for the link"() {
        given: "the link in the XML response doesn't contain enough tokens to be updated"
        def requestUrl = "/fail-index/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlBodyWithLink(responseBodyLink)) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code == HttpServletResponse.SC_INTERNAL_SERVER_ERROR as String
    }
}


