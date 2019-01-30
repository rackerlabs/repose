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

import groovy.xml.MarkupBuilder
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@Category(Filters)
class UriStripperLinkResourceXmlTest extends ReposeValveTest {

    def static String tenantId = "138974928"
    def static xmlSlurper = new XmlSlurper()
    def static responseHeaders = ["Content-Type": MimeType.APPLICATION_XML.toString()]

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

    def "when the uri does not match the configured uri-path-regex, the response body should not be modified"() {
        given: "url does not match any configured uri-path-regex"
        def requestUrl = "/matches/nothing/$tenantId/some/more/resources"

        and: "an XML response body contains a link without a tenantId"
        def responseBodyLink = "/matches/nothing/some/more/resources"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

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
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

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
        MimeType.APPLICATION_JSON | """{"name":"Requested donation","price":3.5,"tags":["Loch Ness Monster","tree fiddy"]}"""
    }

    @Unroll
    def "the response body link can be updated using the #position token only"() {
        given: "the link in the XML response only contains one of the tokens"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

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
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made and the JSON response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        bookstore.book.link == responseBodyLink
    }

    def "when configured to continue on mismatch, the response body is not modified if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bookstore/>"
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
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                "$xPath" responseBodyLink
            }
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, stringWriter.toString()) }

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
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
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
        def responseHandler = { request -> new Response(200, null, responseHeaders, stringWriter.toString()) }

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
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        bookstore.book.link.isEmpty() // link should have been removed
        bookstore.book.author == "Some person"  // unrelated field should remain unaltered
    }

    def "when configured to remove on mismatch, an attribute in the response body should be removed"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/remove-attribute/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        bookstore.book.'@category'.isEmpty() // link should have been removed
    }

    def "when configured to remove on mismatch, the response body is not modified if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/remove/foo/$tenantId/bar"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
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
        (mc.receivedResponse.body as String).replace("\n", "") == body.replace("\n", "")
    }

    def "when configured to remove on mismatch, the response body link is removed if the token index is too high for the link"() {
        given: "the link in the XML response doesn't contain enough tokens"
        def requestUrl = "/remove-index/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is not modified"
        bookstore.book.link.isEmpty() // link should have been removed
        bookstore.book.author == "Some person"  // unrelated field should remain unaltered
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the response body link can't be updated due to no recognizable tokens"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/fail/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the xpath to the attribute does not resolve"() {
        given: "the link in the XML response doesn't contain the previous nor following token"
        def requestUrl = "/fail-attribute/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c/d/e/f/g/h"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink("non/matching/link")) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the xpath to the link does not resolve"() {
        given: "the XML response doesn't contain the link field at all"
        def requestUrl = "/fail/foo/$tenantId/bar"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
            }
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, stringWriter.toString()) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the token index is too high for the link"() {
        given: "the link in the XML response doesn't contain enough tokens to be updated"
        def requestUrl = "/fail-index/foo/$tenantId/bar"
        def responseBodyLink = "/a/b/c"
        def responseHandler = { request -> new Response(200, null, responseHeaders, simpleXmlWithLink(responseBodyLink)) }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)

        then: "the response code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    @Unroll
    def "when configured XPath is #xPath, the book at index #index in the XML response body should have its link updated"() {
        given: "the XML response body has the configured link"
        def requestUrl = "/xpath/$url/$tenantId/bar"
        def responseHandler = { request -> new Response(200, null, responseHeaders, body) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        bookstore.book[index]."$node" == "/xpath/foo/$tenantId/bar"

        where:
        xPath                                           | index | node        | url                | body
        "/bookstore/book/link"                          | 0     | "link"      | "basic"            | simpleXmlWithLink("/xpath/foo/bar")
        "/bookstore//link"                              | 0     | "link"      | "any-child"        | simpleXmlWithLink("/xpath/foo/bar")
        "/bookstore/book/@category"                     | 0     | "@category" | "attribute"        | simpleXmlWithLink("/xpath/foo/bar")
        "/bookstore/book[1]/link"                       | 1     | "link"      | "first"            | xmlWithMultipleBooks()
        "/bookstore/book[price>45]/link"                | 2     | "link"      | "element-search"   | xmlWithMultipleBooks()
        "/bookstore/book[@category='BAKING']/link"      | 0     | "link"      | "attribute-search" | xmlWithMultipleBooks()
        "/bookstore/book[not(@category='FLIGHT')]/link" | 0     | "link"      | "function-not"     | xmlWithMultipleBooks()
        "/bookstore/book[last()]/link"                  | 3     | "link"      | "function-last"    | xmlWithMultipleBooks()
        "//link[@*]"                                    | 3     | "link"      | "attribute-any"    | xmlWithMultipleBooks()
    }

    @Unroll
    def "when configured XPath is #xPath, all of the books in the XML response body should have their link updated"() {
        given: "the XML response body has the configured link"
        def requestUrl = "/xpath/$url/$tenantId/bar"
        def responseHandler = { request -> new Response(200, null, responseHeaders, xmlWithMultipleBooks()) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        (0..3).each { assert bookstore.book[it].link == "/xpath/foo/$tenantId/bar" }

        where:
        url                        | xPath
        "all-links-double-slash"   | "//link"
        "all-links-bookstore-star" | "/bookstore/*/link"
        "all-links-using-or"       | "/bookstore/book[0]/link | /bookstore/book[1]/link | /bookstore/book[2]/link | /bookstore/book[3]/link"
    }

    def "when the XML response body has a namespace, the response body link is updated"() {
        given: "the XML response body has the configured link and a namespace"
        def requestUrl = "/namespacex/foo/$tenantId/bar"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.'x:bookstore'('xmlns:x': 'http://www.groovy-lang.org') {
            'x:book'(category: 'BAKING') {
                'x:title' lang: 'en', 'Everyday French'
                'x:author' 'Some person'
                'x:year' 2001
                'x:price' 20.00
                'x:link' '/namespacex/foo/bar'
            }
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, stringWriter.toString()) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)
                .declareNamespace(x: 'http://www.groovy-lang.org')

        then: "the response body link is modified"
        bookstore.'x:book'.'x:link' == "/namespacex/foo/$tenantId/bar"
    }

    def "when the request URL matches multiple link-resources, all are applied to the response body"() {
        given: "the request URL will match multiple link-resources"
        def requestUrl = "/multi-url-a/multi-url-b/$tenantId/bar"

        and: "the XML response body has the configured links in the response body"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                "link-a" "/multi-url-a/multi-url-b/bar"
                "link-b" "/multi-url-a/multi-url-b/bar"
            }
        }
        def responseHandler = { request -> new Response(200, null, responseHeaders, stringWriter.toString()) }

        when: "a request is made and the XML response body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl, defaultHandler: responseHandler)
        def bookstore = xmlSlurper.parseText(mc.receivedResponse.body as String)

        then: "the response body link is modified"
        bookstore.book."link-a" == requestUrl
        bookstore.book."link-b" == requestUrl
    }

    def simpleXmlWithLink(String responseBodyLink) {
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)

        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: responseBodyLink) {
                title lang: 'en', 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                link responseBodyLink
            }
        }

        stringWriter.toString()
    }

    def xmlWithMultipleBooks() {
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)

        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: 'BAKING') {
                title lang: 'en', 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                link '/xpath/foo/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Flying Airplanes'
                author 'That other person'
                year 2004
                price 39.99
                link '/xpath/foo/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Sick Tricks for Planes'
                author 'Ed'
                author 'Edd'
                author 'Eddy'
                year 2000
                price 59.95
                link '/xpath/foo/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Flying More Airplanes'
                author 'That other person'
                year 2006
                price 39.99
                link lang: 'en', '/xpath/foo/bar'
            }
        }

        stringWriter.toString()
    }
}


