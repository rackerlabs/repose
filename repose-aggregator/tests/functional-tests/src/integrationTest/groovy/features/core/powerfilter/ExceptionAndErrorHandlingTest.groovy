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
package features.core.powerfilter

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

import javax.servlet.http.HttpServletResponse

import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR

class ExceptionAndErrorHandlingTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/powerfilter/exceptionanderror", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def "should return a 500 response if an exception is thrown by a filter during processing"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/exceptionanderror"
        )

        then:
        // The PowerFilterChain catches the Exception and sets the response code to 500
        messageChain.receivedResponse.code.toInteger() == SC_INTERNAL_SERVER_ERROR
    }

    def "should return a 502 response if an error is thrown by a filter during processing"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint + "/exceptionanderror",
            headers: ["X-Throw-Error": "true"]
        )

        then:
        // The PowerFilter catches the Error and sets the response code to 502
        messageChain.receivedResponse.code.toInteger() == SC_BAD_GATEWAY
    }
}
