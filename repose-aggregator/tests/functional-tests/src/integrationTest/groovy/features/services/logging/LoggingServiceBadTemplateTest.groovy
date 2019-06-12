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

import org.junit.After
import org.junit.Before
import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockValkyrie
import org.openrepose.framework.test.server.CustomizableSocketServerConnector
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Endpoint
import scaffold.category.Filters
import scaffold.category.Services
import spock.lang.Shared
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static javax.servlet.http.HttpServletResponse.SC_OK

@Category(Services)
class LoggingServiceBadTemplateTest extends ReposeValveTest {
    def static originEndpoint
    @Shared
    Map params

    @Before
    def setup() {
        super.setupSpec()
        deproxy = new Deproxy()
        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/logging/common", params)
    }

    @After
    def tearDown() {
        super.cleanupSpec()
    }

    @Unroll
    def "Should log a message if a JSON template #message to validate"() {
        given: "Repose is started without a #message HTTP Logging Service config"
        repose.configurationProvider.applyConfigs("features/services/logging/$configs", params)
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1

        and: "a message should be logged"
        reposeLogSearch.awaitByString("$message to validate JSON")

        where:
        message  | configs
        "Failed" | "bad"
        "Unable" | "good"
    }

    def "Should start and run without a configuration"() {
        given: "Repose is started without an HTTP Logging Service config"
        repose.start()
        waitUntilReadyToServiceRequests()

        when: "Request is sent through repose"
        def messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
        )

        then: "Repose should return an OK (200)"
        messageChain.receivedResponse.code as Integer == SC_OK

        and: "The request should have reached the origin service"
        messageChain.handlings.size() == 1
    }
}
