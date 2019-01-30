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

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import scaffold.category.Core
import spock.lang.Unroll

@Category(Core)
class ConnectionManagementTest extends ReposeValveTest {

    String charset = (('A'..'Z') + ('0'..'9')).join()

    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/requestsize", params)
        repose.start()
    }

    @Unroll("When sending a #reqMethod with a huge body through repose returns a 413")
    def "should return 413 on request body that is too large"() {
        given: "I have a request body that exceed the header size limit"
        def body = makeLargeString(32100)

        when: "I send a request to REPOSE with my request body"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, requestBody: body, method: reqMethod)

        then: "I get a response of 413"
        mc.receivedResponse.code == "413"
        mc.handlings.size() == 0


        where:
        reqMethod | _
        "POST"    | _
        "PUT"     | _
        "DELETE"  | _
        "PATCH"   | _
    }

    def "Should pass content-encoding header"() {

        when: "Request goes through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, headers: ["content-encoding": "gzip"])

        then: "repose should not remove header"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getCountByName("content-encoding") == 1
        mc.handlings[0].request.headers["content-encoding"] == "gzip"

    }

    //Stolen from: http://stackoverflow.com/a/2474496/423218
    def makeLargeString(int size) {
        StringBuilder sb = new StringBuilder(size)
        (0..size).each { count ->
            sb.append(randomChar())
        }
        sb.toString()
    }

    //Stolen from http://stackoverflow.com/a/2627897/423218
    def randomChar() {
        int rnd = (int) (Math.random() * 52)
        char base = (rnd < 26) ? 'A' : 'a'
        return (char) (base + rnd % 26)
    }
}
