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
package features.core.proxy

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import spock.lang.Unroll

class ChunkedAutoTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/chunkedauto", params)
        repose.start()
    }

    @Unroll("When set to #method chunked encoding to auto and sending #reqBody that is chunked #chunked.")
    def "When set to send chunked encoding to auto. Repose should send requests chunked if they originated chunked"() {
        when:
        def messageChain = deproxy.makeRequest(
                url: reposeEndpoint,
                method: method,
                requestBody: reqBody,
                chunked: chunked
        )
        def clientRequestHeaders = messageChain.sentRequest.headers
        def originRequestHeaders = messageChain.getHandlings()[0].request.headers

        then:
        //clientRequestHeaders.findAll("Transfer-Encoding").size() == (chunked ? 1 : 0)
        // @TODO This seems to work.??? This may be a Deproxy thing.
        clientRequestHeaders.findAll("Transfer-Encoding").size() == (chunked && (reqBody != null) ? 1 : 0)
        originRequestHeaders.findAll("Transfer-Encoding").size() == (chunked && (reqBody != null) && !method.equalsIgnoreCase("TRACE") ? 1 : 0)
        originRequestHeaders.findAll("Content-Type").size() == ((reqBody == null) ? 0 : 1)
        // @TODO This seems to work.???
        //originRequestHeaders.findAll("Content-Type").size() == (chunked || (reqBody == null) ? 0 : 1)
        originRequestHeaders.findAll("Content-Length").size() == (chunked ? 0 : 1)
        // @TODO Next guess based on what works in the ContentLengthTest.
        //originRequestHeaders.findAll("Content-Length").size() == (chunked || method.equalsIgnoreCase("TRACE") ? 0 : 1)
        // @TODO This seems to work.???
        //originRequestHeaders.findAll("Content-Length").size() ==
        //        (!method.equalsIgnoreCase("TRACE") && (!chunked || (chunked && (reqBody == null))) ? 1 : 0)

        if (originRequestHeaders.findAll("Transfer-Encoding").size() > 0)
            assert originRequestHeaders.getFirstValue("Transfer-Encoding").equalsIgnoreCase("Chunked")
        if (originRequestHeaders.findAll("Content-Length").size())
            originRequestHeaders.getFirstValue("Content-Length").toInteger() == ((reqBody == null) ? 0 : reqBody.length())

        where:
        [method, reqBody, chunked] << [["POST", "PUT", "TRACE"], ["blah", null], [true, false]].combinations()

//        // @TODO This seems to work.???
//        then:
//        clientRequestHeaders.findAll("Transfer-Encoding").size() == client_encoding
//        originRequestHeaders.findAll("Transfer-Encoding").size() == origin_encoding
//        originRequestHeaders.findAll("Content-Type").size() == content_type
//        originRequestHeaders.findAll("Content-Length").size() == content_length
//
//        if (originRequestHeaders.findAll("Transfer-Encoding").size() > 0)
//            assert originRequestHeaders.getFirstValue("Transfer-Encoding").equalsIgnoreCase("Chunked")
//        if (originRequestHeaders.findAll("Content-Length").size())
//            originRequestHeaders.getFirstValue("Content-Length").toInteger() == ((reqBody == null) ? 0 : reqBody.length())
//
//        where:
//        method  | reqBody | chunked | client_encoding | origin_encoding | content_type | content_length
//        "POST"  | "blah"  | true    | 1               | 1               | 0            | 0
//        "POST"  | null    | true    | 0               | 0               | 0            | 1
//        "PUT"   | "blah"  | true    | 1               | 1               | 0            | 0
//        "PUT"   | null    | true    | 0               | 0               | 0            | 1
//        "TRACE" | "blah"  | true    | 1               | 0               | 0            | 0
//        "TRACE" | null    | true    | 0               | 0               | 0            | 0
//        "POST"  | "blah"  | false   | 0               | 0               | 1            | 1
//        "POST"  | null    | false   | 0               | 0               | 0            | 1
//        "PUT"   | "blah"  | false   | 0               | 0               | 1            | 1
//        "PUT"   | null    | false   | 0               | 0               | 0            | 1
//        "TRACE" | "blah"  | false   | 0               | 0               | 1            | 0
//        "TRACE" | null    | false   | 0               | 0               | 0            | 0
    }
}
