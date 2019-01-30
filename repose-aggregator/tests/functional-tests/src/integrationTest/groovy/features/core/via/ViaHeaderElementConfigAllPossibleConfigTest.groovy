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

package features.core.via

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import scaffold.category.Core
import scaffold.category.Slow
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Shared
import spock.lang.Unroll

import static org.springframework.http.HttpHeaders.VIA

/**
 * Tests the various possibilities for how the via-header element can be configured and their effect on the request and
 * response Via header values. This test has to start Repose for each configuration possibility.
 */
@Category(Core)
class ViaHeaderElementConfigAllPossibleConfigTest extends ReposeValveTest {

    @Shared String reposePort
    @Shared String reposeVersion

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort, 'origin service')

        reposePort = properties.reposePort as String
        reposeVersion = properties.reposeVersion
    }

    def cleanup() {
        repose?.stop()
    }

    String generateViaHeaderAttribute(String requestPrefix, String responsePrefix, Boolean reposeVersion) {
        (requestPrefix ? /request-prefix="$requestPrefix" / : "") +
            (responsePrefix ? /response-prefix="$responsePrefix" / : "") +
            (reposeVersion != null ? /repose-version="$reposeVersion" / : "")
    }

    def startReposeWithConfigParams(Map testParams = [:]) {
        Map params = properties.defaultTemplateParams + testParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/via", params)
        repose.configurationProvider.applyConfigs("features/core/via/elementtemplate", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "request-prefix of #requestPrefixDesc, response-prefix of <undefined>, and repose-version of false results in request header '#expectedRequestHeader' and no response header"() {
        given: "Repose is started with the specified configuration"
        startReposeWithConfigParams(viaHeaderAttributes: generateViaHeaderAttribute(requestPrefix, null, false))

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "the response to the client should not have a Via header"
        !mc.receivedResponse.headers.contains(VIA)

        and: "the request to the origin service should contain a Via header with the expected value"
        mc.handlings[0].request.headers.getFirstValue(VIA) == expectedRequestHeader

        where:
        requestPrefix | expectedRequestHeader
        null          | "1.1 localhost:$reposePort (Repose/$reposeVersion)"
        "Lighthouse"  | "1.1 Lighthouse (Repose/$reposeVersion)"

        requestPrefixDesc = requestPrefix ? "'$requestPrefix'" : "<undefined>"
    }

    @Unroll
    def "request-prefix of #requestPrefixDesc, response-prefix of #responsePrefixDesc, and repose-version of #resposeVersionDesc results in request header '#expectedRequestHeader' and response header '#expectedResponseHeader'"() {
        given: "Repose is started with the specified configuration"
        startReposeWithConfigParams(viaHeaderAttributes: generateViaHeaderAttribute(requestPrefix, responsePrefix, version))

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint)

        then: "the response to the client should contain the expected value"
        mc.receivedResponse.headers.getFirstValue(VIA) == expectedResponseHeader

        and: "the request to the origin service should contain a Via header with the expected value"
        mc.handlings[0].request.headers.getFirstValue(VIA) == expectedRequestHeader

        where:
        requestPrefix | responsePrefix | version | expectedRequestHeader                               | expectedResponseHeader
        null          | null           | null    | "1.1 localhost:$reposePort (Repose/$reposeVersion)" | "1.1 Repose (Repose/$reposeVersion)"
        null          | null           | true    | "1.1 localhost:$reposePort (Repose/$reposeVersion)" | "1.1 Repose (Repose/$reposeVersion)"
        "Potato"      | null           | null    | "1.1 Potato (Repose/$reposeVersion)"                | "1.1 Repose (Repose/$reposeVersion)"
        "Potato"      | null           | true    | "1.1 Potato (Repose/$reposeVersion)"                | "1.1 Repose (Repose/$reposeVersion)"
        null          | "Salad"        | null    | "1.1 localhost:$reposePort (Repose/$reposeVersion)" | "1.1 Salad (Repose/$reposeVersion)"
        null          | "Salad"        | true    | "1.1 localhost:$reposePort (Repose/$reposeVersion)" | "1.1 Salad (Repose/$reposeVersion)"
        "Potato"      | "Salad"        | null    | "1.1 Potato (Repose/$reposeVersion)"                | "1.1 Salad (Repose/$reposeVersion)"
        "Potato"      | "Salad"        | true    | "1.1 Potato (Repose/$reposeVersion)"                | "1.1 Salad (Repose/$reposeVersion)"
        null          | "Salad"        | false   | "1.1 localhost:$reposePort (Repose/$reposeVersion)" | "1.1 Salad"
        "Potato"      | "Salad"        | false   | "1.1 Potato (Repose/$reposeVersion)"                | "1.1 Salad"

        requestPrefixDesc = requestPrefix ? "'$requestPrefix'" : "<undefined>"
        responsePrefixDesc = responsePrefix ? "'$responsePrefix'" : "<undefined>"
        resposeVersionDesc = version != null ? "'$version'" : "<undefined>"
    }
}
