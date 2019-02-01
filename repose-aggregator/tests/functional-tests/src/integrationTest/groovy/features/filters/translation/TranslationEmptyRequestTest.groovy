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
import spock.lang.Unroll

@Category(XmlParsing)
class TranslationEmptyRequestTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String rssPayload = "<a>test body</a>"
    def
    static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def
    static String xmlPayloadWithExtEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [  <!ENTITY license_agreement SYSTEM \"http://www.mydomain.com/license.xml\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&license_agreement;</a>"
    def
    static String xmlPayloadWithXmlBomb = "<?xml version=\"1.0\"?> <!DOCTYPE lolz [   <!ENTITY lol \"lol\">   <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">   <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">   <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">   <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">   <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">   <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">   <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">   <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\"> ]> <lolz>&lol9;</lolz>"
    def static String jsonPayload = "{\"field1\": \"value1\", \"field2\": \"value2\"}"
    def static String invalidXml = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJson = "{{'field1': \"value1\", \"field2\": \"value2\"]}"


    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map contentRss = ["content-type": "application/rss+xml"]

    def
    static ArrayList<String> xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]


    def String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/empty", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    @Unroll("response: xml, request: #reqHeaders - #reqBody")
    def "when translating requests"() {

        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders, requestBody: reqBody, defaultHandler: xmlResp)
        def sentRequest = ((MessageChain) resp).handlings[0]

        then: "Request body sent from repose to the origin service should contain"

        resp.receivedResponse.code == responseCode

        if (responseCode != "400") {
            if (sentRequest.request.body instanceof byte[])
                assert (convertStreamToString(sentRequest.request.body) == requestToOrigin)
            else
                assert (sentRequest.request.body == requestToOrigin)
        }

        where:
        reqHeaders                 | reqBody                   | requestToOrigin                                | method | responseCode
        acceptXML + contentXML     | xmlPayLoad                | xmlPayLoad                                     | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithEntities    | "<a><remove-me>test</remove-me>\"somebody</a>" | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithXmlBomb     | null                                           | "POST" | '400'
        acceptXML + contentXMLHTML | xmlPayLoad                | xmlPayLoad                                     | "POST" | '200'
        acceptXML + contentXMLHTML | xmlPayLoad                | xmlPayLoad                                     | "PUT"  | '200'
        acceptXML + contentJSON    | jsonPayload               | jsonPayload                                    | "POST" | '200'
        acceptXML + contentOther   | jsonPayload               | jsonPayload                                    | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithExtEntities | "<a><remove-me>test</remove-me>\"somebody</a>" | "POST" | "200"


    }

    @Unroll("Requests - headers: #headerName with \"#headerValue\" keep its case")
    def "Requests - headers should keep its case in requests"() {

        when: "make a request with the given header and value"
        def headers = [
                'Content-Length': '0'
        ]
        headers[headerName.toString()] = headerValue.toString()

        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: headers)

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
