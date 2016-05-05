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

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class ChunkedTest extends ReposeValveTest {


    def setupSpec() {

        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/proxy", params)
        repose.configurationProvider.applyConfigs("features/services/httpconnectionpool/withconfig", params)
        repose.start()
    }

    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }

        if (repose) {
            repose.stop()
        }
    }

    @Unroll("When set to #method chunked encoding to true and sending #reqBody.")
    def "When set to send chunked encoding to true. Repose should send requests chunked"() {

        when:
        MessageChain messageChain = deproxy.makeRequest([url: reposeEndpoint, method: method, requestBody: reqBody])
        def sentRequest = ((MessageChain) messageChain).getHandlings()[0]

        then:
        ((Handling) sentRequest).request.getHeaders().findAll("transfer-encoding").size() == transfer_encoding
        ((Handling) sentRequest).request.getHeaders().findAll("content-type").size() == content_type
        ((Handling) sentRequest).request.getHeaders().findAll("content-length").size() == content_length

        if (transfer_encoding > 0)
            assert ((Handling) sentRequest).request.getHeaders().getFirstValue("transfer-encoding").equalsIgnoreCase("chunked")

        where:
        method  | reqBody | content_type | content_length | transfer_encoding
        "POST"  | "blah"  | 1            | 0              | 1
        "POST"  | null    | 0            | 0              | 1
        "PUT"   | "blah"  | 1            | 0              | 1
        "PUT"   | null    | 0            | 0              | 1
        "TRACE" | "blah"  | 1            | 0              | 0
        "TRACE" | null    | 0            | 0              | 0

    }


}
