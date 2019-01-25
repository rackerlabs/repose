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
import org.rackspace.deproxy.Handlers
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import spock.lang.Unroll

/**
 * This test shows that the HTTP Client Service can be configured to make use of a proxy.
 *
 * As a means of testing, we are using a filter that in turn uses the HTTP Client Service.
 * When an HTTP client is configured with a proxy, the proxy should receive any requests that
 * would otherwise be sent to the target determined by the filter.
 * A filter is used rather than the origin service so that we can select which filter, and
 * thus which HTTP client, to be tested without having to reload any configuration.
 * Unfortunately, this does couple this test to the filter being used (which may require
 * additional test setup that would not otherwise be required), but this is preferable
 * to having to reload configuration for each proxy configuration under test.
 */
class HttpClientServiceGoodProxyTest extends ReposeValveTest {

    final static String encodedBasicAuthCredentials = Base64.encoder.encode("username:password".getBytes())

    static int originServiceEndpointCount
    static int identityEndpointCount
    static int proxyEndpointCount

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(
            port: properties.targetPort,
            defaultHandler: { Request request ->
                originServiceEndpointCount++
                Handlers.simpleHandler(request)
            }
        )
        deproxy.addEndpoint(
            port: properties.identityPort,
            defaultHandler: { Request request ->
                identityEndpointCount++
                Handlers.simpleHandler(request)
            }
        )
        deproxy.addEndpoint(
            port: properties.targetPort2,
            defaultHandler: { Request request ->
                proxyEndpointCount++
                Handlers.simpleHandler(request)
            }
        )

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/proxy/good", params)
        repose.start()
    }

    def setup() {
        originServiceEndpointCount = 0
        identityEndpointCount = 0
        proxyEndpointCount = 0
    }

    @Unroll("should support proxies for the filter associated with #path")
    def "the HTTP Client Service should support proxies"() {
        given: "the connection pool used by the filter which triggers on this request path is configured with a proxy"

        when: "a request is made to Repose"
        MessageChain messageChain = deproxy.makeRequest(
            url: reposeEndpoint,
            path: path,
            headers: [
                Authorization: "Basic $encodedBasicAuthCredentials"
            ]
        )

        then: "the request will be sent to the proxy"
        messageChain.handlings.empty
        messageChain.orphanedHandlings.size() == 1
        originServiceEndpointCount == 0
        identityEndpointCount == 0
        proxyEndpointCount == 1

        where:
        path << [
            "/one",
            // cannot test this proxy URI since it is lacking a port: "/two",
            "/three",
            // cannot test this proxy URI since it is lacking a port: "/four"
        ]
    }
}
