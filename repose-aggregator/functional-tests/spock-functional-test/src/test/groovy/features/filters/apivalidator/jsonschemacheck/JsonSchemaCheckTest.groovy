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
package features.filters.apivalidator.jsonschemacheck

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response

/**
 * Created by jennyvo on 10/2/15.
 */
class JsonSchemaCheckTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/apivalidator/jsonschemacheck", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    def "GET on /path/to/test Json checking"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain
        def customHandler = { return new Response(200, "OK") }

        def Map<String, String> headers = [
                "Accept"         : "application/json",
                "Content-Type"   : "application/json",
                "Host"           : "localhost",
                "User-Agent"     : "gdeproxy"
        ]

        def reqBody = """{
            "firstName" : "Jorge",
            "lastName" : "Williams",
            "age" : 38
            }"""

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/path/to/test",
                method: 'GET', headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then:
        "result should be " + 200
        messageChain.receivedResponse.code.equals("200")
        messageChain.receivedResponse.headers["Content-Type"].equals("application/json")
    }

    def "GET on /path/to/test Json checking Invalid Json"() {
        setup: "declare messageChain to be of type MessageChain"
        MessageChain messageChain
        def customHandler = { return new Response(200, "OK") }

        def Map<String, String> headers = [
                "Accept"         : "application/json",
                "Content-Type"   : "application/json",
                "Host"           : "localhost",
                "User-Agent"     : "gdeproxy"
        ]

        def reqBody = """{
            "firstName" : "Jorge",
            "lastName" : "Williams",
            "age" : 38
            }}"""

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/path/to/test",
                method: 'GET', headers: headers,
                requestBody: reqBody, defaultHandler: customHandler,
                addDefaultHeaders: false
        )

        then:
        "result should be " + 400
        messageChain.receivedResponse.code.equals("400")
        messageChain.receivedResponse.headers["Content-Type"].equals("application/json")
    }
}
