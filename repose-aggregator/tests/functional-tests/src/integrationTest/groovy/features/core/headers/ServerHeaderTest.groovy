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
package features.core.headers

import framework.ReposeConfigurationProvider
import framework.ReposeValveLauncher
import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import spock.lang.Unroll

class ServerHeaderTest extends ReposeValveTest {

    static int originServicePort
    static int reposePort
    static String url
    static ReposeConfigurationProvider reposeConfigProvider

    def setupSpec() {
        deproxy = new Deproxy()
        originServicePort = properties.targetPort
        deproxy.addEndpoint(originServicePort)

        reposePort = properties.reposePort
        url = "http://localhost:${reposePort}"

        reposeConfigProvider = new ReposeConfigurationProvider(configDirectory, configTemplates)
        repose = new ReposeValveLauncher(
                reposeConfigProvider,
                properties.getReposeJar(),
                url,
                properties.getConfigDirectory(),
                reposePort)
        repose.enableDebug()

        def params = properties.getDefaultTemplateParams()

        reposeConfigProvider.cleanConfigDirectory()
        reposeConfigProvider.applyConfigs("common", params)
        reposeConfigProvider.applyConfigs("features/core/headers", params)

        repose.start(
                killOthersBeforeStarting: false,
                waitOnJmxAfterStarting: false)

        repose.waitForNon500FromUrl(url)
    }

    @Unroll("The received response should have the Server header value of \"#headerValue\"")
    def "The received response should have the Server header value of \"#headerValue\""() {

        given: "a set of Origin Service headers"
        def headers = [
                'Content-Length': '0'
        ]
        Optional.ofNullable(headerValue).ifPresent {
            value -> headers[HttpHeaders.SERVER] = value }

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
                url: url,
                defaultHandler: { new Response(HttpStatus.OK, null, headers) })

        then: "it should make it to the origin service"
        mc.handlings.size() == 1

        and: "the response's header should have the correct value"
        // TODO: Current behavior is to remove all Server headers coming from the Origin Service.
        //       This is will be corrected as part of: https://repose.atlassian.net/browse/REP-5320
        //mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == expectedCount
        //mc.receivedResponse.headers.getFirstValue(HttpHeaders.SERVER) == headerValue
        mc.receivedResponse.headers.getCountByName(HttpHeaders.SERVER) == 0

        where:
        headerValue           | expectedCount
        "OriginServiceHeader" | 1
        null                  | 0
    }
}
