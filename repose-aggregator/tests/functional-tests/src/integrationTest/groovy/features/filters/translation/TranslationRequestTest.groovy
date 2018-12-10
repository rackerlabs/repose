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

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class TranslationRequestTest extends ReposeValveTest {

    static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    static String rssPayload = "<a>test body</a>"
    static String xmlPayloadWithEntities =
        """<?xml version="1.0" standalone="no" ?>
          |<!DOCTYPE a [
          |   <!ENTITY c SYSTEM  "/etc/passwd">
          |]>
          |<a>
          |   <remove-me>test</remove-me>&quot;somebody&c;
          |</a>
          |""".stripMargin()
    static String xmlPayloadWithExtEntities =
        """<?xml version="1.0" standalone="no" ?>
          |<!DOCTYPE a [
          |   <!ENTITY license_agreement SYSTEM "http://www.mydomain.com/license.xml">
          |]>
          |<a>
          |   <remove-me>test</remove-me>&quot;somebody&license_agreement;
          |</a>
          |""".stripMargin()
    static String xmlPayloadWithXmlBomb =
        """<?xml version="1.0"?>
          |<!DOCTYPE lolz [
          |   <!ENTITY lol0 "lol">
          |   <!ENTITY lol1 "&lol0;&lol0;&lol0;&lol0;&lol0;&lol0;&lol0;&lol0;&lol0;&lol0;">
          |   <!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
          |   <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
          |   <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
          |   <!ENTITY lolX "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol0;&lol0;">
          |]>
          |<lolz>&lolX;</lolz>
          |""".stripMargin()
    static String jsonPayload = """{"field1": "value1", "field2": "value2"}"""
    static String invalidXml = "<a><remove-me>test</remove-me>somebody"
    static String invalidJson = """{{'field1': "value1", "field2": "value2"]}"""

    static Map acceptXML = ["accept": "application/xml"]
    static Map contentXML = ["content-type": "application/xml"]
    static Map contentJSON = ["content-type": "application/json"]
    static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    static Map contentOther = ["content-type": "application/other"]
    static Map contentRss = ["content-type": "application/rss+xml"]

    static ArrayList<String> xmlJSON = [
        """<json:string name="field1">value1</json:string>""",
        """<json:string name="field2">value2</json:string>"""
    ]
    static String remove = "remove-me"
    static String add = "add-me"

    String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next()
    }

    //Start repose once for this particular translation test
    def setupSpec() {
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/request", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
    }

    @Unroll("response: xml, request: #reqHeaders - #reqBody")
    def "when translating requests"() {
        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }

        when: "User passes a request through repose"
        def resp = deproxy.makeRequest(
            url: (String) reposeEndpoint,
            method: method,
            headers: reqHeaders,
            requestBody: reqBody,
            defaultHandler: xmlResp
        )
        def sentRequest = ((MessageChain) resp).handlings[0]

        then: "Received response code should be appropriate"
        resp.receivedResponse.code == responseCode

        and: "Request body sent from repose to the origin service should contain"
        if (responseCode != "400") {
            for (String st : shouldContain) {
                if (sentRequest.request.body instanceof byte[])
                    assert (convertStreamToString(sentRequest.request.body as byte[]).contains(st))
                else
                    assert (sentRequest.request.body.contains(st))
            }
        }

        and: "Request body sent from repose to the origin service should not contain"
        if (responseCode != "400") {
            for (String st : shouldNotContain) {
                if (sentRequest.request.body instanceof byte[])
                    assert (!convertStreamToString(sentRequest.request.body as byte[]).contains(st))
                else
                    assert (!sentRequest.request.body.contains(st))
            }
        }

        where:
        reqHeaders                 | reqBody                   | shouldContain   | shouldNotContain | method | responseCode
        acceptXML + contentXML     | xmlPayLoad                | ["somebody"]    | [remove]         | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithEntities    | ["\"somebody"]  | [remove]         | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithXmlBomb     | ["\"somebody"]  | [remove]         | "POST" | '400'
        acceptXML + contentXMLHTML | xmlPayLoad                | [add]           | []               | "POST" | '200'
        acceptXML + contentXMLHTML | xmlPayLoad                | [xmlPayLoad]    | [add]            | "PUT"  | '200'
        acceptXML + contentJSON    | jsonPayload               | [add] + xmlJSON | []               | "POST" | '200'
        acceptXML + contentOther   | jsonPayload               | [jsonPayload]   | [add]            | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithExtEntities | ["\"somebody"]  | [remove]         | "POST" | "200"
    }

    def "when translating application/rss+xml requests with header translations"() {
        given: "Repose is configured to translate request headers"
        def respHeaders = ["content-type": "application/xml"]
        def reqHeaders = ['test': 'x', 'other': 'y']
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, rssPayload) }

        when: "User sends a request through repose"
        def resp = deproxy.makeRequest(
            url: (String) reposeEndpoint + "/somepath?testparam=x&otherparam=y",
            method: "POST",
            headers: contentRss + acceptXML + reqHeaders,
            requestBody: rssPayload,
            defaultHandler: xmlResp
        )
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body sent from repose to the origin service should contain"
        ((Handling) sentRequest).request.body.contains(rssPayload)
        ((Handling) sentRequest).request.path.contains("otherparam=y")
        resp.receivedResponse.code == "200"
        !((Handling) sentRequest).request.body.contains("add-me")
        !((Handling) sentRequest).request.path.contains("testparam=x")

        and: "Request headers sent from repose to the origin service should contain"
        ((Handling) sentRequest).request.headers.getNames().contains("translation-header")
        ((Handling) sentRequest).request.headers.getNames().contains("other")
        !((Handling) sentRequest).request.headers.getNames().contains("test")
    }

    def "when attempting to translate an invalid xml/json request"() {
        when: "User passes invalid json/xml through repose"
        def resp = deproxy.makeRequest(
            url: (String) reposeEndpoint,
            method: "POST",
            headers: reqHeaders,
            requestBody: reqBody,
            defaultHandler: ""
        )

        then: "Repose will send back 400s as the requests are invalid"
        resp.receivedResponse.code == respCode

        where:
        reqHeaders              | respHeaders | reqBody     | respCode
        acceptXML + contentXML  | contentXML  | invalidXml  | "400"
        acceptXML + contentJSON | contentXML  | invalidJson | "400"
    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {
        when: "make a request with the given header and value"
        def reqHeaders = [
            'Content-Length': '0',
            (headerName)    : headerValue
        ]

        MessageChain mc = deproxy.makeRequest(
            url: reposeEndpoint,
            headers: reqHeaders
        )

        then: "the request should keep headerName and headerValue case"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.contains(headerName)
        mc.handlings[0].request.headers.getFirstValue(headerName) == headerValue

        where:
        headerName         | headerValue
        "Accept"           | "text/plain"
        "ACCEPT"           | "text/PLAIN"
        "accept"           | "TEXT/plain;q=0.2"
        "aCCept"           | "text/plain"
        "CONTENT-Encoding" | "identity"
        "Content-ENCODING" | "identity"
        //"content-encoding" | "idENtItY"
        //"Content-Encoding" | "IDENTITY"
    }
}
