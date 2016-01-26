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

package org.openrepose.commons.utils.logging

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64
import org.junit.Before
import org.slf4j.MDC
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by Mario on 8/17/15.
 */
class TracingHeaderHelperTest extends Specification {

    @Before
    def setup() {
        MDC.clear();
    }

    @Unroll
    def 'get Request ID from Base64 encoded JSON string: #jsonContents'() {
        given:
        def tracingHeader = Base64.encodeBase64String(JsonOutput.toJson(jsonContents).bytes)

        when:
        def requestId = TracingHeaderHelper.getTraceGuid(tracingHeader)

        then:
        requestId == jsonContents.requestId

        where:
        jsonContents << [
                [requestId:'5c65235b-06a4-4461-ad09-c1db3dae81b7'],
                [requestId:'4ceac78d-bf1e-4b18-b788-87da5f05388b', sessionId:'a54b0120-4352-4d53-b89b-a5c328122616'],
                [requestId:'36a15d3c-b485-4350-84b3-b60327f659fc', sessionId:'bac9ff48-a944-4ee9-816c-a57d0fdb2e3e', user:'bob', domain:'rackspace'],
                [requestId:'b3082092-ef75-44aa-9597-f7e830304a8b', favoriteFood:'french fries', favoriteSeason:'summer'],
                [edgeCase:'Request ID Missing']
        ]
    }

    @Unroll
    def 'get Request ID from logging context when it is available despite tracing header contents of #tracingHeader'() {
        given:
        MDC.put('traceGuid', uuid)

        when:
        def requestId = TracingHeaderHelper.getTraceGuid(tracingHeader)

        then:
        requestId == uuid

        where:
        uuid                                   | tracingHeader
        '57d81803-3d80-4fce-a07a-a6b569441dc7' | null
        'c59d9ea2-4994-4927-a7d2-3234d15062df' | ''
        '8d0c84fe-9862-4c57-8d88-047338b97a50' | 'potato'
        'cce4a344-ec37-46f6-a41a-79202d31de07' | 'SV9MSUtFX0hBTQ=='
        '2f410bd5-686c-43b1-bb23-7068751c3c7c' | '{"requestId":"7fbf1c74-7ba8-44b1-8914-8bb01466af9b"}'
        '54476536-bb80-45d0-9331-596fe5e98889' | Base64.encodeBase64String(JsonOutput.toJson([requestId:'683229c1-929c-4414-87d4-24693edb7446']).bytes)
    }

    @Unroll
    def 'get Request ID from entire string when header is not properly encoded: #tracingHeader'() {
        when:
        def requestId = TracingHeaderHelper.getTraceGuid(tracingHeader)

        then:
        notThrown(Exception)
        requestId == tracingHeader

        where:
        tracingHeader << [null, '', 'potato', 'SV9MSUtFX0hBTQ==']
    }

    def 'get Request ID from entire string when header is a UUID (legacy)'() {
        given:
        def uuid = '88c51bb3-2897-47ea-8403-fa1fd5fc17ca'

        when:
        def requestId = TracingHeaderHelper.getTraceGuid(uuid)

        then:
        requestId == uuid
    }

    def 'create Tracing Header from specified Request ID and Origin'() {
        given:
        def requestId = '67564a55-1fd0-43f9-ab9c-7055c9334f30'
        def origin = 'some_via'

        when:
        def tracingHeader = TracingHeaderHelper.createTracingHeader(requestId, origin)

        then:
        new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).requestId == requestId
        new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).origin == origin
        !new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).user
    }

    def 'create Tracing Header from specified Request ID, Origin, and Username'() {
        given:
        def requestId = '67564a55-1fd0-43f9-ab9c-7055c9334f30'
        def origin = 'some_via'
        def username = 'Sam'

        when:
        def tracingHeader = TracingHeaderHelper.createTracingHeader(requestId, origin, username)

        then:
        new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).requestId == requestId
        new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).origin == origin
        new JsonSlurper().parseText(new String(Base64.decodeBase64(tracingHeader))).user == username
    }

    def 'decode Tracing Header to printable string'() {
        given:
        def jsonString = JsonOutput.toJson([requestId:'4ceac78d-bf1e-4b18-b788-87da5f05388b', sessionId:'a54b0120-4352-4d53-b89b-a5c328122616'])
        def tracingHeader = Base64.encodeBase64String(jsonString.bytes)

        when:
        def decodedTracingHeader = TracingHeaderHelper.decode(tracingHeader)

        then:
        decodedTracingHeader == jsonString
    }

    def 'decode is skipped and the same string is returned if string is not properly Base64 encoded'() {
        given:
        def tracingHeader = '{"requestId":"04721d61-7420-4472-a5a6-ebfa0ac485bd"}'

        when:
        def decodedTracingHeader = TracingHeaderHelper.decode(tracingHeader)

        then:
        decodedTracingHeader == tracingHeader
    }
}
