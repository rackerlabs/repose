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
package features.filters.cors

import framework.ReposeValveTest
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll
import static org.junit.Assert.*

/**
 * Created by jennyvo on 9/29/15.
 */
class CorsFilterBasicTest extends ReposeValveTest {
    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/cors", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll ("Cors origin allow method: #method")
    def "When send request with cors filter the specific headers should be added"() {

        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: method)

        then:
        mc.receivedResponse.code == "200"
        mc.getHandlings().size() == 1
        mc.handlings[0].request.getHeaders().findAll(CommonHttpHeader.ORIGIN).size() == 1
        mc.handlings[0].request.getHeaders().findAll(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD).size() == 1

        where:
        method << ["GET", "POST", "PUT", "DELETE"]
    }

    def "When send request to specify resource" () {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + path, method: method)

        then:
        mc.receivedResponse.code == "200"
        mc.getHandlings().size() == handling
        if (handling == 1) {
            assertTrue(mc.handlings[0].request.getHeaders().findAll(CommonHttpHeader.ACCESS_CONTROL_REQUEST_METHOD).size() == 1)
        }

        where:
        path            | method        | handling
        "/testget/foo"  | "GET"         | 1
        "/testget/boo"  | "GET"         | 1
        "/testget/boo"  | "POST"        | 0
        "/testget/boo"  | "PUT"         | 0
        "/testget/boo"  | "DELETE"      | 0
        "/testpost/boo" | "POST"        | 1
        "/testpost/foo" | "POST"        | 1
        "/testpost/boo" | "GET"         | 0
        "/testpost/boo" | "PUT"         | 0
        "/testpost/boo" | "DELETE"      | 0
    }
}