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
package features.filters.contenttypestripper

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ContentTypeStripperTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/contenttypestripper", params)
        repose.start()
    }

    @Unroll("Reg with method:#method, req body:#requestBody - #desc")
    def "should maintain the content-type header when there is a body #desc and maintain the integrity of the body on a #method request"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: ["content-type": "text/plain"], method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("Content-Type").size() == 1
        ((Handling) sentRequest).request.body == (method == "GET" ? "" : requestBody) //We remove the body on a get when finally calling the service

        where:
        desc                     | requestBody       | method
        "over 8 characters"      | "I like pie"      | "GET"
        "over 8 characters"      | "I like pie"      | "PUT"
        "over 8 characters"      | "I like pie"      | "POST"
        "over 8 characters"      | "I like pie"      | "DELETE"
        "less than 8 characters" | " Pie "           | "GET"
        "less than 8 characters" | " Pie "           | "PUT"
        "less than 8 characters" | " Pie "           | "POST"
        "less than 8 characters" | " Pie "           | "DELETE"
        "xml body"               | "<tag>test</tag>" | "POST"
    }

    @Unroll("Remove content-type Reg with method:#method, req body:#requestBody - #desc")
    def "should remove the content-type header when there #desc for #method requests"() {
        when:
        def messageChain = deproxy.makeRequest([url: reposeEndpoint, requestBody: requestBody, headers: ["content-type": "apple/pie"], method: method])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("Content-Type").size() == 0
        ((Handling) sentRequest).request.body == (method == "GET" ? "" : requestBody) //We remove the body on a get when finally calling the service
        // Verify Repose not throw error log for non anotation filter
        reposeLogSearch.searchByString("Requested filter, *.ContentTypeStriperFilter is not an annotated Component. Make sure your filter is an annotated Spring Bean.").size() == 0


        where:
        desc                                                          | requestBody                           | method
        "is no body"                                                  | ""                                    | "GET"
        "is no body"                                                  | ""                                    | "POST"
        "is no body"                                                  | ""                                    | "PUT"
        "is no body"                                                  | ""                                    | "DELETE"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "GET"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "POST"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "PUT"
        "is only whitespace in the first 8 characters"                | " \n \r \t  "                         | "DELETE"
        "is only whitespace in the first 8 characters even with text" | " \n \r \t  unfortunately heres text" | "POST"
        "is a less than 8 character white space body"                 | "    "                                | "POST"
    }
}
