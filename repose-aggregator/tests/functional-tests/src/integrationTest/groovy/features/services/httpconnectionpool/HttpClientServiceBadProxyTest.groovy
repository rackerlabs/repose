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
package features.services.httpconnectionpool

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openrepose.framework.test.ReposeLauncher.MAX_STARTUP_TIME

/**
 * This test shows that the HTTP Client Service will fail to update when an invalid
 * proxy is configured.
 */
class HttpClientServiceBadProxyTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        deproxy.addEndpoint(properties.targetPort2)
    }

    def "the HTTP Client Service should fail to update if the configured proxy is invalid"() {
        given: "Repose is started with an invalid proxy configured for an HTTP Client"
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/proxy/bad", params)
        repose.start(waitOnJmxAfterStarting: false)

        expect: "a message to be logged informing that user the the proxy is invalid"
        reposeLogSearch.awaitByString("Configuration update error. Reason: Invalid HTTP host", 1, MAX_STARTUP_TIME, SECONDS)
    }
}
