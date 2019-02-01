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
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.XmlParsing
import spock.lang.Unroll

@Category(XmlParsing)
class TranslationMultiMatchTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String simpleXml = "<a>test body</a>"
    def static Map acceptXML = ["accept": "application/xml"]
    def static Map acceptOther = ["accept": "application/other"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentAtom = ["content-type": "application/atom+xml"]
    def static String remove = "remove-me"
    def static String add = "add-me"
    def testHeaders = ['test': 'x', 'other': 'y']

    def String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    //Start repose once for this particular translation test
    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/multimatch", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("response headers: #reqHeaders")
    def "when translating responses"() {
        given: "Repose is configured to translate responses using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }

        when:
        "The origin service sends back a response of type " + respHeaders
        def resp = deproxy.makeRequest(
                url: reposeEndpoint,
                path: '/echobody?testparam=x&otherparam=y',
                method: "GET",
                headers: reqHeaders + testHeaders,
                requestBody: respBody,
                defaultHandler: xmlResp
        )

        then: "Response body received should contain"
        for (String st : shouldContain) {
            if (resp.receivedResponse.body instanceof byte[])
                assert (convertStreamToString(resp.receivedResponse.body).contains(st))
            else
                assert (resp.receivedResponse.body.contains(st))
        }

        and: "Response body received should not contain"
        if (resp.receivedResponse.body instanceof byte[])
            assert (!convertStreamToString(resp.receivedResponse.body).contains("remove-me"))
        else
            assert (!resp.receivedResponse.body.contains("remove-me"))

        and: "Response headers should contain"
        resp.receivedResponse.getHeaders().getNames().contains(headerContain)

        and: "Response headers should not contain"
        !resp.receivedResponse.getHeaders().getNames().contains(headerDoesNotContain)

        and: "Response code returned should be"
        resp.receivedResponse.code == "200"

        where:
        reqHeaders                | respHeaders               | respBody                                      | shouldContain           | headerContain            | headerDoesNotContain
        acceptXML + contentXML    | contentXML                | "<root>" + xmlPayLoad + simpleXml + "</root>" | ["somebody", simpleXml] | "translation-response-a" | "translation-response-b"
        acceptOther + contentAtom | contentAtom + acceptOther | simpleXml                                     | [simpleXml]             | "translation-response-b" | "translation-response-a"
    }

    @Unroll("request headers: #reqHeaders")
    def "when translating request headers"() {
        given: "Repose is configured to translate requests using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }

        when:
        "The user sends a request of type " + reqHeaders
        def resp = deproxy.makeRequest(
                url: (String) reposeEndpoint,
                method: "POST",
                headers: reqHeaders,
                requestBody: simpleXml,
                defaultHandler: xmlResp
        )
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body from repose to the origin service should contain"
        sentRequest.request.body.contains(simpleXml)

        and: "Request headers sent from repose to the origin service should contain"
        for (String st : shouldContainHeaders) {
            assert (((Handling) sentRequest).request.getHeaders().getNames().contains(st))
        }

        and: "Request headers sent from repose to the origin service should not contain "
        for (String st : shouldNotContainHeaders) {
            assert (!((Handling) sentRequest).request.getHeaders().getNames().contains(st))
        }

        where:
        reqHeaders              | shouldContain | shouldContainHeaders               | shouldNotContainHeaders
        acceptXML + contentXML  | [simpleXml]   | ["translation-a", "translation-b"] | []
        acceptXML + contentAtom | [simpleXml]   | ["translation-b"]                  | ["translation-a"]
    }

    @Unroll("request: #reqHeaders")
    def "when translating multi-match requests"() {
        given: "Repose is configured to translate requests using multimatch"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }

        when:
        "The user sends a request of type " + reqHeaders
        def resp = deproxy.makeRequest(
                url: reposeEndpoint,
                path: "/somepath?testparam=x&otherparam=y",
                method: "POST",
                headers: reqHeaders + testHeaders,
                requestBody: simpleXml,
                defaultHandler: xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body from repose to the origin service should contain"
        resp.receivedResponse.code == "200"
        sentRequest.request.path == requestPath
        sentRequest.request.body.contains(simpleXml)

        and: "Request headers sent from repose to the origin service should contain"
        for (String st : shouldContainHeaders) {
            assert (((Handling) sentRequest).request.getHeaders().getNames().contains(st))
        }

        and: "Request headers sent from repose to the origin service should not contain "
        for (String st : shouldNotContainHeaders) {
            assert (!((Handling) sentRequest).request.getHeaders().getNames().contains(st))
        }

        where:
        reqHeaders              | shouldContainHeaders               | shouldNotContainHeaders | requestPath
        acceptXML + contentAtom | ["translation-b"]                  | ["translation-a"]       | "/somepath?translation-b=b&testparam=x&otherparam=y"
        acceptXML + contentXML  | ["translation-a", "translation-b"] | []                      | "/somepath?translation-b=b&translation-a=a&testparam=x&otherparam=y"
    }
}
