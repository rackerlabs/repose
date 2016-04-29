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
package features.filters.bodyextractortoheader

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

import static org.junit.Assert.*;

/**
 * Created by jennyvo on 4/28/16.
 */
class BodyExtractorToHeaderTest extends ReposeValveTest {
    def setupSpec() {
        reposeLogSearch.cleanLog()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/bodyextractortoheader", params)
        repose.start()
        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    def static params
    def static matchDevideBody = """{"bodydata": {"name":"test", "device": "12345", "something": "foo"}}"""
    def static matchServerBody = """{"bodydata": {"name":"test", "server": "abc123", "something": "foo"}}"""
    def static noNatchBody = """{"bodydata": {"name":"test", "something": "foo"}}"""

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    @Unroll()
    def "When request with match config jpath will add to header as config"() {
        when:
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint, method: "POST", requestBody: reqbody)

        then:
        mc.handlings.size() == 1
        if (expectedheaders == "") {
            assertFalse(mc.handlings[0].request.headers.contains("x-device-id"))
            assertFalse(mc.handlings[0].request.headers.contains("x-server-id"))
        } else {
            assertTrue(mc.handlings[0].request.headers.contains(expectedheaders))
            assertEquals(mc.handlings[0].request.headers.getFirstValue(expectedheaders), headervalue)
        }

        where:
        reqbody         | expectedheaders | headervalue
        matchDevideBody | "x-device-id"   | "12345"
        matchServerBody | "x-server-id"   | "abc123"
        noNatchBody     | ""              | ""
    }
}
