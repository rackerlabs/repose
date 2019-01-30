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
package features.filters.translation

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.XmlParsing

@Category(XmlParsing)
class TranslationHeadersQueriesTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a>test</a>"
    def static String rssPayload = "<a>test body</a>"
    def
    static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String jsonPayload = "{\"a\":\"1\",\"b\":\"2\"}"
    def static String invalidXml = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJson = "{{'field1': \"value1\", \"field2\": \"value2\"]}"


    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map contentRss = ["content-type": "application/rss+xml"]

    def
    static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/headersQueries", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    def "when translating request headers"() {

        when: "User passes a request through repose"
        def resp =
                deproxy.makeRequest([
                        url        : (String) reposeEndpoint,
                        method     : "POST",
                        headers    : acceptXML + contentXML,
                        requestBody: xmlPayLoad])
        def handling = ((MessageChain) resp).getHandlings()[0]

        then: "Request headers sent from repose to the origin service should contain"
        handling.request.headers.contains("extra-header")

    }

    def "when translating request query parameters"() {

        given: "Repose is configured to translate request query params"
        def xmlResp = { request -> new Response(200, "OK", contentXML) }


        when: "User passes a request through repose"
        def resp =
                deproxy.makeRequest(
                        url: (String) reposeEndpoint + "/path/to/resource/",
                        method: "POST",
                        headers: acceptXML + contentJSON,
                        requestBody: jsonPayload,
                        defaultHandler: xmlResp)
        def handling = ((MessageChain) resp).getHandlings()[0]

        then: "Request url sent from repose to the origin service should contain"
        handling.getRequest().path.contains("extra-query=result")
    }


}
