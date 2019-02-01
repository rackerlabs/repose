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
package features.filters.bodypatcher

import groovy.json.JsonSlurper
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 5/10/16.
 *   Body Patch Json Test RFC-6902
 */
@Category(Filters)
class BodyPatcherTest extends ReposeValveTest {

    def static bodyJson1 = """{"bar": "test", "banana": "2"}"""
    def static bodyJson2 = """{"bar": "test", "banana": "2", "test":{"bar": "test", "foo": "2"}}"""
    def static bodyJson3 = """{"bar": "test", "banana": "2", "array":["one", "four", "two", "three"]}"""
    def static respBodyJson1 = """{"foo": "test", "test": "8"}"""

    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/bodypatcher", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll("OP with Request path, headers: #headers")
    def "OP applying patches on Request"() {
        when: "send request match replace path"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", headers: headers, requestBody: bodyJson1)
        def body = new String(mc.handlings[0].request.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then:
        result["bar"] == "boo"
        result["hello"][0] == "world"
        result["banana"] == null
        mc.receivedResponse.code == "200"

        where:
        headers << [["Content-Type": "application/json"], ["Content-Type": "application/json+atom"], ["Content-Type": "json"]]
    }

    @Unroll("More OP with Request path, headers: #headers")
    def "More OP applying patches on Request"() {
        when: "send request match replace path"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/test/", method: "POST", headers: headers, requestBody: bodyJson2)
        def body = new String(mc.handlings[0].request.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then:
        result.test["bar"] == "boo"
        result.test["hello"][0] == "world"
        result.test["fooo"] == "2"
        mc.receivedResponse.code == "200"

        where:
        headers << [["Content-Type": "application/json"], ["Content-Type": "application/json+atom"], ["Content-Type": "json"]]
    }

    def "OP applying patches on Response"() {
        given:
        def Map headers = ["content-type": "test/json"]
        "Json Response from origin service"
        def jsonResp = { request -> return new Response(200, "OK", ["content-type": "application/json"], respBodyJson1) }

        when: "send request match replace path"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "GET", headers: headers,
                defaultHandler: jsonResp, requestBody: bodyJson1)

        def body = new String(mc.receivedResponse.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then:
        mc.handlings.size() == 1
        result["test"] == "repose"
        result["bar"] == "test"
        mc.receivedResponse.code == "200"
    }

    def "More OP Working with Array"() {
        when: "send request match replace path"
        MessageChain mc = deproxy.makeRequest(url: "$reposeEndpoint/array/", method: "POST", headers: ["content-type": "application/json"], requestBody: bodyJson3)
        def body = new String(mc.handlings[0].request.body)
        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        then:
        result.array[0] == "one"
        result.array[1] == "two"
        result.array[2] == "three"
        result.array[3] == null
        mc.receivedResponse.code == "200"
    }
}
