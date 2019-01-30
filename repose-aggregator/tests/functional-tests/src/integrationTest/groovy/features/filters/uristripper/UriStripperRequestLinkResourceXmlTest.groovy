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
import scaffold.category.Filters
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

@Category(Filters)
class UriStripperRequestLinkResourceXmlTest extends ReposeValveTest {

    def static String tenantId = "138974928"
    def static xmlSlurper = new XmlSlurper()
    def static requestHeaders = ["Content-Type": MimeType.APPLICATION_XML.toString()]

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/common", params)
        repose.configurationProvider.applyConfigs("features/filters/uristripper/linkresource/request/xml", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def "when the uri does not match the configured uri-path-regex, the request body should not be modified"() {
        given: "url does not match any configured uri-path-regex"
        def requestUrl = "/matches/nothing/$tenantId/some/more/resources"

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: simpleXmlWithLink(requestUrl))
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the link in the request body should remain unmodified"
        bookstore.book.link == requestUrl
    }

    @Unroll
    def "when the HTTP method #method is used, the request body link should be #expectedRequestBodyLink"() {
        given: "an XML request body contains a link without a tenantId"
        def requestUrl = "/continue/foo/$tenantId/bar"

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: method,
                headers: requestHeaders,
                requestBody: simpleXmlWithLink(requestUrl))
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is the expected value for the given method"
        bookstore.book.link == expectedRequestBodyLink

        where:
        method   | expectedRequestBodyLink
        "DELETE" | "/continue/foo/$tenantId/bar"
        "POST"   | "/continue/foo/bar"
        "PUT"    | "/continue/foo/$tenantId/bar"
        "PATCH"  | "/continue/foo/$tenantId/bar"
    }

    @Unroll
    def "when the request is #contentType, the request body is not modified"() {
        given: "a request body"
        def requestUrl = "/continue/foo/$tenantId/bar"

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: ["Content-Type": contentType.toString()],
                requestBody: body)

        then: "the request body is not modified"
        mc.handlings[0].request.body as String == body

        where:
        contentType               | body
        MimeType.TEXT_PLAIN       | "There's a million things I haven't done, just you wait"
        MimeType.APPLICATION_JSON | """{"name":"Requested donation","price":3.5,"tags":["Loch Ness Monster","tree fiddy"]}"""
    }

    def "when configured to continue on mismatch, the request body is not modified if the xpath to the link does not resolve"() {
        given: "the XML request doesn't contain the link field at all"
        def requestUrl = "/continue/foo/$tenantId/bar"
        def body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bookstore/>"

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: body)

        then: "the request body is not modified"
        mc.handlings[0].request.body as String == body
    }

    @Unroll
    def "when configured to update the token-index #index, the XML request body link should be updated from #requestBodyLink to #modifiedRequestBodyLink"() {
        given: "the XML request body has the configured link"
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
                "$xPath" requestBodyLink
            }
        }

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        bookstore.book."$xPath" == modifiedRequestBodyLink

        where:
        xPath    | index | requestBodyLink              | modifiedRequestBodyLink
        "link-a" | 0     | "/$tenantId"                 | "/"
        "link-a" | 0     | "/$tenantId/a/b/c/d/e/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-b" | 1     | "/a/$tenantId/b/c/d/e/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/$tenantId/f/g/h" | "/a/b/c/d/e/f/g/h"
        "link-c" | 5     | "/a/b/c/d/e/$tenantId"       | "/a/b/c/d/e"
        "link-c" | 5     | "/a/b/c"                     | "/a/b/c"
    }

    def "when an XML request body contains multiple link-resources, they are all updated"() {
        given: "the XML request body contains multiple link-resources"
        def requestUrl = "/continue-index/foo/$tenantId/bar"
        def expectedLink = "/a/b/c/d/e/f/g/h"
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
                "link-a" "/$tenantId/a/b/c/d/e/f/g/h"
                "link-b" "/a/$tenantId/b/c/d/e/f/g/h"
                "link-c" "/a/b/c/d/e/$tenantId/f/g/h"
            }
        }

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "all of the links in the request body are updated correctly"
        bookstore.book."link-a" == expectedLink
        bookstore.book."link-b" == expectedLink
        bookstore.book."link-c" == expectedLink
    }

    def "when configured to remove on mismatch, the request body is not modified if the xpath to the link does not resolve"() {
        given: "the XML request doesn't contain the link field at all"
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

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: body)

        then: "the request body is not modified"
        (mc.handlings[0].request.body as String).replace("\n", "") == body.replace("\n", "")
    }

    def "when configured to remove on mismatch, the request body link is removed if the token index is too high for the link"() {
        given: "the link in the XML request doesn't contain enough tokens"
        def requestUrl = "/remove-index/foo/$tenantId/bar"
        def requestBodyLink = "/a/b/c"

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: simpleXmlWithLink(requestBodyLink))
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is not modified"
        bookstore.book.link.isEmpty() // link should have been removed
        bookstore.book.author == "Some person"  // unrelated field should remain unaltered
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the xpath to the attribute does not resolve"() {
        given: "the link in the XML request doesn't contain the previous nor following token"
        def requestUrl = "/fail-attribute/foo/$tenantId/bar"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book {
                title lang: 'en', 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
            }
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())

        then: "the request code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_BAD_REQUEST
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the xpath to the link does not resolve"() {
        given: "the XML request doesn't contain the link field at all"
        def requestUrl = "/fail/foo/$tenantId/bar"
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)
        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book {
                title 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
            }
        }

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())

        then: "the request code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_BAD_REQUEST
    }

    def "when configured to fail on mismatch, Repose returns a 500 if the token index is too high for the link"() {
        given: "the link in the XML request doesn't contain enough tokens to be updated"
        def requestUrl = "/fail-index/foo/$tenantId/bar"
        def requestBodyLink = "/a/b/c"

        when: "a request is made"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: simpleXmlWithLink(requestBodyLink))

        then: "the request code is 500"
        mc.receivedResponse.code as Integer == HttpServletResponse.SC_BAD_REQUEST
    }

    @Unroll
    def "when configured XPath is #xPath, the book at index #index in the XML request body should have its link updated"() {
        given: "the XML request body has the configured link"
        def requestUrl = "/xpath/$url/$tenantId/bar"

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: body)
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        bookstore.book[index]."$node" == "/xpath/foo/bar"

        where:
        xPath                                           | index | node        | url                | body
        "/bookstore/book/link"                          | 0     | "link"      | "basic"            | simpleXmlWithLink("/xpath/foo/$tenantId/bar")
        "/bookstore//link"                              | 0     | "link"      | "any-child"        | simpleXmlWithLink("/xpath/foo/$tenantId/bar")
        "/bookstore/book/@category"                     | 0     | "@category" | "attribute"        | simpleXmlWithLink("/xpath/foo/$tenantId/bar")
        "/bookstore/book[1]/link"                       | 1     | "link"      | "first"            | xmlWithMultipleBooks()
        "/bookstore/book[price>45]/link"                | 2     | "link"      | "element-search"   | xmlWithMultipleBooks()
        "/bookstore/book[@category='BAKING']/link"      | 0     | "link"      | "attribute-search" | xmlWithMultipleBooks()
        "/bookstore/book[not(@category='FLIGHT')]/link" | 0     | "link"      | "function-not"     | xmlWithMultipleBooks()
        "/bookstore/book[last()]/link"                  | 3     | "link"      | "function-last"    | xmlWithMultipleBooks()
        "//link[@*]"                                    | 3     | "link"      | "attribute-any"    | xmlWithMultipleBooks()
    }

    @Unroll
    def "when configured XPath is #xPath, all of the books in the XML request body should have their link updated"() {
        given: "the XML request body has the configured link"
        def requestUrl = "/xpath/$url/$tenantId/bar"

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: xmlWithMultipleBooks())
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        (0..3).each { assert bookstore.book[it].link == "/xpath/foo/bar" }

        where:
        url                        | xPath
        "all-links-double-slash"   | "//link"
        "all-links-bookstore-star" | "/bookstore/*/link"
        "all-links-using-or"       | "/bookstore/book[0]/link | /bookstore/book[1]/link | /bookstore/book[2]/link | /bookstore/book[3]/link"
    }

    def "when the XML request body has a namespace, the request body link is updated"() {
        given: "the XML request body has the configured link and a namespace"
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
                'x:link' "/namespacex/foo/$tenantId/bar"
            }
        }

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)
                .declareNamespace(x: 'http://www.groovy-lang.org')

        then: "the request body link is modified"
        bookstore.'x:book'.'x:link' == "/namespacex/foo/bar"
    }

    def "when the request URL matches multiple link-resources, all are applied to the request body"() {
        given: "the request URL will match multiple link-resources"
        def requestUrl = "/multi-url-a/multi-url-b/$tenantId/bar"

        and: "the XML request body has the configured links in the request body"
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
                "link-a" requestUrl
                "link-b" requestUrl
            }
        }

        when: "a request is made and the XML request body is parsed"
        def mc = deproxy.makeRequest(url: reposeEndpoint + requestUrl,
                method: "POST",
                headers: requestHeaders,
                requestBody: stringWriter.toString())
        def bookstore = xmlSlurper.parseText(mc.handlings[0].request.body as String)

        then: "the request body link is modified"
        bookstore.book."link-a" == "/multi-url-a/multi-url-b/bar"
        bookstore.book."link-b" == "/multi-url-a/multi-url-b/bar"
    }

    def simpleXmlWithLink(String requestBodyLink) {
        def stringWriter = new StringWriter()
        def xmlBuilder = new MarkupBuilder(stringWriter)

        xmlBuilder.setDoubleQuotes(true)
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")
        xmlBuilder.bookstore {
            book(category: requestBodyLink) {
                title lang: 'en', 'Everyday French'
                author 'Some person'
                year 2001
                price 20.00
                link requestBodyLink
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
                link '/xpath/foo/$tenantId/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Flying Airplanes'
                author 'That other person'
                year 2004
                price 39.99
                link '/xpath/foo/$tenantId/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Sick Tricks for Planes'
                author 'Ed'
                author 'Edd'
                author 'Eddy'
                year 2000
                price 59.95
                link '/xpath/foo/$tenantId/bar'
            }
            book(category: 'FLIGHT') {
                title lang: 'en', 'Flying More Airplanes'
                author 'That other person'
                year 2006
                price 39.99
                link lang: 'en', '/xpath/foo/$tenantId/bar'
            }
        }

        stringWriter.toString()
    }
}


