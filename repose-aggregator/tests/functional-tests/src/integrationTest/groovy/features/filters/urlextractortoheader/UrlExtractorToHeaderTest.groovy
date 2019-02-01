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
package features.filters.urlextractortoheader

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/23/15.
 *  url-extractor-to-header test
 */
@Category(Filters)
class UrlExtractorToHeaderTest extends ReposeValveTest {
    def static originEndpoint
    def static Map params = [:]

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params);
        repose.configurationProvider.applyConfigs("features/filters/urlextractortoheader", params);

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    @Unroll("#method with deviceID: #deviceID should return a #responseCode")
    def "Test regex extract deviceId from url set to header"() {
        given: "A device ID in the url request"

        when: "a request is made against a device"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/resource/" + deviceID, method: method,
                headers: [
                        'content-type': 'application/json',
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.getFirstValue("x-device-id") == deviceID
        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")

        where:
        method   | deviceID | responseCode
        "GET"    | "520707" | "200"
        "HEAD"   | "520708" | "200"
        "PUT"    | "520708" | "200"
        "POST"   | "520709" | "200"
        "PATCH"  | "520710" | "200"
        "DELETE" | "520711" | "200"
    }

    @Unroll("#method with serverID: #serverID should return a #responseCode")
    def "Test regex extract server name from url set to header"() {
        given: "A server ID in the url request"

        when: "a request is made against a device"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/v2.0/servers/" + serverID, method: method,
                headers: [
                        'content-type': 'application/json',
                ]
        )

        then: "check response"
        mc.receivedResponse.code == responseCode
        mc.handlings.get(0).request.headers.contains("x-server-id")
        mc.handlings.get(0).request.headers.getFirstValue("x-server-id") == serverID
        //**This for tracing header on failed response REP-2147
        mc.receivedResponse.headers.contains("x-trans-id")

        where:
        method   | serverID     | responseCode
        "GET"    | "test520707" | "200"
        "HEAD"   | "test520708" | "200"
        "PUT"    | "ser_520708" | "200"
        "POST"   | "sss-520709" | "200"
        "PATCH"  | "sss:520710" | "200"
        "DELETE" | "ebc.520711" | "200"
    }
}
