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
}


