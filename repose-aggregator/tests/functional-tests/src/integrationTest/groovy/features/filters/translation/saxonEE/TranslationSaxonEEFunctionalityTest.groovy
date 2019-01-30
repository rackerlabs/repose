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
package features.filters.translation.saxonEE

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.XmlParsing
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

@Category(XmlParsing)
class TranslationSaxonEEFunctionalityTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a>test</a>"
    def static String jsonPayload = "{\"a\":\"1\",\"b\":\"2\"}"
    def
    static String jsonInXmlId9 = "<entry xml:lang=\"en\" xmlns=\"http://www.w3.org/2005/Atom\">    <category term=\"image.upload\"/>    <category term=\"DATACENTER=ord1\"/>    <category term=\"REGION=preprod-ord\"/>    <content type=\"application/json\"> {  \"event_type\": \"image.upload\",  \"timestamp\": \"2013-04-09 23:18:57.557571\",  \"message_id\": \"b\",  \"payload\": {    \"updated_at\": \"2013-04-09T23:18:57\",    \"id\": \"9\"    } }    </content> </entry>"
    def
    static String jsonInXmlId8 = "<entry xml:lang=\"en\" xmlns=\"http://www.w3.org/2005/Atom\">    <category term=\"image.upload\"/>    <category term=\"DATACENTER=ord1\"/>    <category term=\"REGION=preprod-ord\"/>    <content type=\"application/json\"> {  \"event_type\": \"image.upload\",  \"timestamp\": \"2013-04-09 23:18:57.557571\",  \"message_id\": \"b\",  \"payload\": {    \"updated_at\": \"2013-04-09T23:18:57\",    \"id\": \"8\"    } }    </content> </entry>"


    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]


    def
    static String xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String remove = "remove-me"
    def static String add = "add-me"

    //Start repose once for this particular translation test
    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def saxonHome = System.getenv("SAXON_HOME")

        //If we're the jenkins user, set it, and see if it works
        if (saxonHome == null && System.getenv("LOGNAME").equals("jenkins")) {
            //For jenkins, it's going to be in $HOME/saxon_ee
            def home = System.getenv("HOME")
            saxonHome = "${home}/saxon_ee"
            repose.addToEnvironment("SAXON_HOME", saxonHome)
        }

        assert saxonHome != null

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/saxonEE", params)
        repose.start()
    }

    def "when translating json within xml in the request body"() {

        given: "Repose is configured to translate request headers"
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, "") }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint, method: method, headers: reqHeaders, requestBody: reqBody, defaultHandler: xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request headers sent from repose to the origin service should contain"

        for (String st : bodyShouldContain) {
            assert (((Handling) sentRequest).request.body.contains(st))

        }

        where:
        reqHeaders             | respHeaders | reqBody      | method | bodyShouldContain
        acceptXML + contentXML | contentXML  | jsonInXmlId9 | "POST" | ["<category term=\"DATACENTER=req1\"/>", "<category term=\"REGION=req\"/>"]

    }

    def "when translating json within xml in the response body"() {

        given:
        "Origin service returns body of type " + respHeaders
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, respBody) }


        when: "User sends requests through repose"
        def resp = deproxy.makeRequest(url: (String) reposeEndpoint, method: "PUT", headers: reqHeaders, requestBody: "something", defaultHandler: xmlResp)

        then: "Response body should contain"
        for (String st : shouldContain) {
            assert (resp.receivedResponse.body.contains(st))
        }

        and: "Response code should be"
        resp.receivedResponse.code.equalsIgnoreCase(respCode.toString())

        where:
        reqHeaders | respHeaders | respBody     | respCode | shouldContain
        acceptXML  | contentXML  | jsonInXmlId8 | 200      | ["<category term=\"REGION=resp\"/>", "<category term=\"DATACENTER=resp1\"/>"]


    }

}
