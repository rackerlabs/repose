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

import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import spock.lang.Unroll

class UrlExtractorToHeaderMultiTest extends ReposeValveTest {
    def static originEndpoint
    static Map params = [:]

    def setupSpec() {
        deproxy = new Deproxy()
        reposeLogSearch.cleanLog()

        params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/urlextractortoheader", params)
        repose.configurationProvider.applyConfigs("features/filters/urlextractortoheader/multi", params)

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
    }

    @Unroll
    def "#method with deviceID's: #deviceIdOne and #deviceIdTwo should capture both"() {
        given: "a request with multiple device ID's in the url"

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/multi:$deviceIdOne/entities/$deviceIdTwo",
            method: method,
            headers: [
                'content-type': 'application/json',
            ]
        )

        then: "the response should have the appropriate header and values"
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.findAll("x-device-id").sort() ==
            ([deviceIdOne, deviceIdTwo] as List<String>).sort()

        where:
        method   | deviceIdOne  | deviceIdTwo
        "GET"    | "2018111111" | "20181111AA"
        "HEAD"   | "2018111112" | "20181111AB"
        "PUT"    | "2018111113" | "20181111AC"
        "POST"   | "2018111114" | "20181111AD"
        "PATCH"  | "2018111115" | "20181111AE"
        "DELETE" | "2018111116" | "20181111AF"
    }

    @Unroll
    def "#method with deviceID's: #deviceIdOne and #deviceIdTwo should not capture ignored"() {
        given: "a request with multiple device ID's in the url"

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/ignored:$deviceIdOne/entities/$deviceIdTwo",
            method: method,
            headers: [
                'content-type': 'application/json',
            ]
        )

        then: "the response should have the appropriate header and values"
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.findAll("x-device-id") ==
            [deviceIdTwo] as List<String>

        where:
        method   | deviceIdOne  | deviceIdTwo
        "GET"    | "2018111211" | "20181112AA"
        "HEAD"   | "2018111212" | "20181112AB"
        "PUT"    | "2018111213" | "20181112AC"
        "POST"   | "2018111214" | "20181112AD"
        "PATCH"  | "2018111215" | "20181112AE"
        "DELETE" | "2018111216" | "20181112AF"
    }

    @Unroll
    def "#method with deviceID's: #deviceIdOne and #deviceIdTwo should only capture the first partial match."() {
        given: "a request with multiple device ID's in the url"

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/partial:$deviceIdOne/entities/partial:$deviceIdTwo",
            method: method,
            headers: [
                'content-type': 'application/json',
            ]
        )

        then: "the response should have the appropriate header and values"
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.findAll("x-device-id") ==
            [deviceIdOne] as List<String>

        where:
        method   | deviceIdOne  | deviceIdTwo
        "GET"    | "2018111311" | "20181113AA"
        "HEAD"   | "2018111312" | "20181113AB"
        "PUT"    | "2018111313" | "20181113AC"
        "POST"   | "2018111314" | "20181113AD"
        "PATCH"  | "2018111315" | "20181113AE"
        "DELETE" | "2018111316" | "20181113AF"
    }

    @Unroll
    def "#method with deviceID's: #deviceIdOne and #deviceIdTwo should capture both within the boundary"() {
        given: "a request with multiple device ID's in the url"

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/full:$deviceIdOne/entities/$deviceIdTwo",
            method: method,
            headers: [
                'content-type': 'application/json',
            ]
        )

        then: "the response should have the appropriate header and values"
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.findAll("x-device-id").sort() ==
            ([deviceIdOne, deviceIdTwo] as List<String>).sort()

        where:
        method   | deviceIdOne  | deviceIdTwo
        "GET"    | "2018111411" | "20181114AA"
        "HEAD"   | "2018111412" | "20181114AB"
        "PUT"    | "2018111413" | "20181114AC"
        "POST"   | "2018111414" | "20181114AD"
        "PATCH"  | "2018111415" | "20181114AE"
        "DELETE" | "2018111416" | "20181114AF"
    }

    @Unroll
    def "#method with deviceID's: #deviceIdOne and #deviceIdTwo should capture both within the optional boundary"() {
        given: "a request with multiple device ID's in the url"

        when: "a request is made"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/v1/full:$deviceIdOne/entities/$deviceIdTwo/",
            method: method,
            headers: [
                'content-type': 'application/json',
            ]
        )

        then: "the response should have the appropriate header and values"
        mc.handlings.get(0).request.headers.contains("x-device-id")
        mc.handlings.get(0).request.headers.findAll("x-device-id").sort() ==
            ([deviceIdOne, deviceIdTwo] as List<String>).sort()

        where:
        method   | deviceIdOne  | deviceIdTwo
        "GET"    | "2018111511" | "20181115AA"
        "HEAD"   | "2018111512" | "20181115AB"
        "PUT"    | "2018111513" | "20181115AC"
        "POST"   | "2018111514" | "20181115AD"
        "PATCH"  | "2018111515" | "20181115AE"
        "DELETE" | "2018111516" | "20181115AF"
    }
}
