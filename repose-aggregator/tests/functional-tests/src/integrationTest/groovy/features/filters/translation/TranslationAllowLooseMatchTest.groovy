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
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/2/14.
 */
class TranslationAllowLooseMatchTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"

    def String convertStreamToString(byte[] input) {
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }

    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/allowloosematch", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    @Unroll
    def "Allow looser matches with content-type: #contenttype with resp from origin #response_from_origin and resp to client #response_to_client"() {
        given:
        def headers = [
            "content-type": contenttype,
            "accept"      : "application/xml"
        ]
        def handler = { request -> return new Response(201, "Created", headers, xmlPayLoad) }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/somepath?testparam=x&otherparam=y",
            method: 'POST', headers: headers,
            requestBody: xmlPayLoad, defaultHandler: handler)

        then:
        mc.handlings.size() == 1
        mc.receivedResponse.code == "201"
        new String(mc.receivedResponse.body) == response_to_client
        new String(mc.handlings[0].response.body) == response_from_origin
        mc.receivedResponse.getHeaders().findAll("Content-Type").size() == 1
        !mc.receivedResponse.body.toString().contains("httpx:unknown-content")

        where:
        contenttype                        | response_from_origin | response_to_client
        "application/atom+xml"             | xmlPayLoad           | "<a>somebody</a>"
        "application/atom+xml; type=event" | xmlPayLoad           | "<a>somebody</a>"
        "application/atom+xml; v=1"        | xmlPayLoad           | "<a>somebody</a>"
        "application/xml; v=1"             | xmlPayLoad           | "<a>somebody</a>"
        "application/xml;"                 | xmlPayLoad           | "<a>somebody</a>"
        "application/xml"                  | xmlPayLoad           | "<a>somebody</a>"
        "application/json"                 | xmlPayLoad           | xmlPayLoad
        "text/plain; v=1"                  | xmlPayLoad           | xmlPayLoad
        "html/text; v=1"                   | xmlPayLoad           | xmlPayLoad
        "foo/plain; v=1"                   | xmlPayLoad           | xmlPayLoad
        "text/*; v=1"                      | xmlPayLoad           | xmlPayLoad
        "*/*; v=1"                         | xmlPayLoad           | xmlPayLoad
        "application/atom"                 | xmlPayLoad           | xmlPayLoad
        "foo/a"                            | xmlPayLoad           | xmlPayLoad
        "foo/x;"                           | xmlPayLoad           | xmlPayLoad
        "foo/x;foo=bar"                    | xmlPayLoad           | xmlPayLoad
        "foo=bar;foo/x"                    | xmlPayLoad           | xmlPayLoad
    }

    @Unroll
    def "Retain only matching content-type: #contenttype"() {
        given:
        def reqHeaders = [
            "accept"      : "application/xml;q=1 , application/json;q=0.5",
            "Content-Type": contenttype
        ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("Content-Type").size() == 1
        mc.handlings[0].request.headers["Content-Type"] == contenttype

        where:
        contenttype << [
            "application/xml+atom; type=event",
            "application/json; v=1",
            "text/plain; */*",
            "foo/x",
            "foo/x;",
            "foo/x;version=1",
            "foo/x;foo=bar,bar=foo,type=foo",
            "foo=bar;foo/x",
            "foo/x;foo=bar,text/plain;v=1.1"
        ]
    }
}
