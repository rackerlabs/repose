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
import org.rackspace.deproxy.Response
import scaffold.category.XmlParsing
import spock.lang.Unroll

@Category(XmlParsing)
class TranslateUncommonHeadersTest extends ReposeValveTest {
    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentTypeAtomXML = ['content-type': 'application/atom+xml']
    def static Map contentTypeXML = ['content-type': 'application/xml']
    def static Map weirdContentTypeAtomXML = ['content-type': 'application/atom+xml; type=entry;charset=utf-8']
    def static Map weirdAcceptAtomXML = ['accept': 'application/atom+xml; type=entry;charset=utf-8']
    def static Map weirdContentTypeXML = ['content-type': 'application/xml; type=entry;charset=utf-8']
    def static Map weirdAcceptXML = ['accept': 'application/xml; type=entry;charset=utf-8']
    def static sampleAtomEntry =
            """<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
            xmlns="http://wadl.dev.java.net/2009/02" 
            xmlns:db="http://docbook.org/ns/docbook" 
            xmlns:error="http://docs.rackspace.com/core/error" 
            xmlns:wadl="http://wadl.dev.java.net/2009/02" 
            xmlns:json="http://json-schema.org/schema#" 
            xmlns:d317e1="http://wadl.dev.java.net/2009/02" 
            xmlns:usage-summary="http://docs.rackspace.com/core/usage-summary">
  <atom:title type="text">Slice Action</atom:title>
  <atom:author>
    <atom:name>Name</atom:name>
  </atom:author>
  <atom:content type="application/xml">
    <event xmlns="http://docs.rackspace.com/core/event" xmlns:csd="http://docs.rackspace.com/event/servers/slice">
      <csd:product huddleId="202" rootPassword="12345678" version="1">
        <csd:sliceMetaData key="key1" value="value1"/>
        <csd:additionalPublicAddress dns1="1.1.1.1" dns2="1.1.1.1" ip="1.1.1.1"/>
      </csd:product>
    </event>
  </atom:content>
</atom:entry>"""

    //Start repose once for this particular translation test
    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/uncommon", params)
        repose.start()
    }

    @Unroll("when translating requests with uncommon headers - #reqHeaders")
    def "when translating requests with uncommon headers"() {

        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, method: method, headers: reqHeaders, requestBody: sampleAtomEntry)

        then: "The requests makes it to the origin service"
        mc.handlings.size() == 1
        mc.sentRequest.body.contains("csd:product")
        mc.handlings[0].request.body.contains("csd:product")

        and: "Repose removes the indicated xml attributes from the request body"
        mc.sentRequest.body.contains("rootPassword")
        mc.sentRequest.body.contains("huddleId")
        !mc.handlings[0].request.body.contains("rootPassword")
        !mc.handlings[0].request.body.contains("huddleId")

        where:
        reqHeaders                                   | method
        acceptXML + contentTypeAtomXML               | "POST"
        weirdAcceptAtomXML + weirdContentTypeAtomXML | "POST"
        acceptXML + contentTypeXML                   | "POST"
        weirdAcceptXML + weirdContentTypeXML         | "POST"
    }

    @Unroll("when translating responses with uncommon headers - #respHeaders")
    def "when translating responses with uncommon headers"() {

        given:
        "Origin service returns body of type " + respHeaders
        def handler = { request -> return new Response(200, "OK", respHeaders, sampleAtomEntry) }


        when: "User sends requests through repose"
        def mc = deproxy.makeRequest(url: reposeEndpoint, headers: reqHeaders, defaultHandler: handler)

        then: "The requests makes it to the origin service"
        mc.handlings.size() == 1
        mc.handlings[0].response.body.contains("csd:product")
        mc.receivedResponse.body.contains("csd:product")

        and: "Repose removes the indicated xml attributes from the response body"
        mc.handlings[0].response.body.contains("rootPassword")
        mc.handlings[0].response.body.contains("huddleId")
        !mc.receivedResponse.body.contains("rootPassword")
        !mc.receivedResponse.body.contains("huddleId")

        where:
        respHeaders                                  | reqHeaders
        weirdContentTypeAtomXML                      | acceptXML
        weirdContentTypeAtomXML + weirdAcceptAtomXML | acceptXML
        weirdContentTypeXML                          | acceptXML
        weirdContentTypeXML + weirdAcceptXML         | acceptXML
    }
}
