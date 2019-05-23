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
package features.services.logging

import org.junit.Before
import org.junit.BeforeClass
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Response
import scaffold.category.Services
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_OK
import static org.openrepose.commons.utils.http.OpenStackServiceHeader.USER_ID

@Category(Services)
class LoggingServiceTest extends ReposeValveTest {

    @BeforeClass
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/logging", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

//    @Before
//    def setup() {
//        reposeLogSearch.cleanLog()
//    }

    def "Should log a static message"() {
        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The static message should have been logged"
        reposeLogSearch.awaitByString("INFO  default-static - Static Value")
    }

    @Unroll
    def "Should log a CLF message for #method"() {
        given:
        def userId = "UserId"
        def path = "/"
        //def code = SC_OK
        def response = "<!DOCTYPE html><html><head><title>title</title></head><body></body></html>"

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: path,
            method: method,
            headers: [
                (USER_ID): userId,
                'X-Trace-Request': 'true'
            ],
            defaultHandler: {
                return new Response(code,
                    null,
                    [
                        (USER_ID): userId
                    ],
                    response
                )
            }
        )

        then: "Repose should return the code"
        messageChain.receivedResponse.code as Integer == code

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "The common-log should have been logged"
        reposeLogSearch.awaitByString("INFO  common-log - 127.0.0.1 $userId \\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}Z\\] \"$method $path HTTP/1.1\" $code ${response.length()}")

        where:
        method   | code
        "GET"    | 461
        "PUT"    | 462
        "POST"   | 463
        "PATCH"  | 464
        "DELETE" | 465
        "TRACE"  | 466
    }
}
