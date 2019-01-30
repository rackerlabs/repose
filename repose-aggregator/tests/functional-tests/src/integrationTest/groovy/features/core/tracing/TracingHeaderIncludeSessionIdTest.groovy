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
package features.core.tracing

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.junit.experimental.categories.Category
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Core

import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Created by jennyvo on 8/10/15.  Updated by Mario on 8/12/15.
 */
@Category(Core)
class TracingHeaderIncludeSessionIdTest extends ReposeValveTest {

    def static final Pattern UUID_PATTERN = ~/\p{XDigit}{8}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{12}/

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV2Service fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/core/tracing", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        reposeLogSearch.cleanLog()
    }

    def setup() {
        sleep 500
        fakeIdentityService.resetHandlers()
    }

    def 'X-Trans-Id header should be added to the request and response when it does not come in externally'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def headers = [
                'content-type': 'application/json',
                'X-Auth-Token': fakeIdentityService.client_token,
                via           : 'some_via']

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        // request
        new JsonSlurper().parseText(new String(Base64.decodeBase64(
                mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId ==~ UUID_PATTERN
        new JsonSlurper().parseText(new String(Base64.decodeBase64(
                mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                origin == 'some_via'

        // response
        new JsonSlurper().parseText(new String(Base64.decodeBase64(
                mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId ==~ UUID_PATTERN

        new JsonSlurper().parseText(new String(Base64.decodeBase64(
                mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                origin == 'some_via'

        // logs
        reposeLogSearch.searchByString('Trans-Id:' +
                new JsonSlurper().parseText(new String(Base64.decodeBase64(
                        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                        requestId)
    }

    def 'X-Trans-Id header should be added to the request and response when the header is an empty string'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): '',
                via                                     : 'some_via']

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        // request
        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId ==~ UUID_PATTERN
        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                origin == 'some_via'

        // response
        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId ==~ UUID_PATTERN

        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                origin == 'some_via'

        // logs
        reposeLogSearch.searchByString('Trans-Id:' +
                new JsonSlurper().parseText(new String(Base64.decodeBase64(
                        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                        requestId)
    }

    def 'Parse externally provided X-Trans-Id header and add the Request ID to the logging context'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingId = UUID.randomUUID().toString()
        def sessionId = UUID.randomUUID().toString()
        def jsonTracingHeader = JsonOutput.toJson([sessionId: sessionId, requestId: tracingId, user: 'a', domain: 'b'])
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.bytes)
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        // request
        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId == tracingId

        // response
        new JsonSlurper().parseText(new String(
                Base64.decodeBase64(
                        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID)))).
                requestId == tracingId

        // logs
        reposeLogSearch.searchByString("Trans-Id:$tracingId")
        reposeLogSearch.searchByString("sessionId.*?:.*?$sessionId")
    }

    def 'Parse externally provided X-Trans-Id header with several fields and add the Request ID to the logging context'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingId = UUID.randomUUID().toString()
        def sessionId = UUID.randomUUID().toString()
        def jsonTracingHeader = JsonOutput.toJson(
                [sessionId: sessionId, requestId: tracingId, user: 'bob', domain: 'pluto', favoriteTree: 'cherry'])
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.bytes)
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader

        // logs
        reposeLogSearch.searchByString(Pattern.quote("Trans-Id:$tracingId"))
        reposeLogSearch.searchByString("sessionId\":\"$sessionId.*favoriteTree\":\"cherry")
    }

    def 'Handle invalid JSON in X-Trans-Id header and add the whole string to the logging context'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingId = UUID.randomUUID().toString()
        def jsonTracingHeader = "{'tracingId': $tracingId, I_LIKE_HAM}".toString()
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.bytes)
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader

        // logs
        reposeLogSearch.searchByString(Pattern.quote("Trans-Id:$tracingHeader"))
        reposeLogSearch.searchByString(Pattern.quote(tracingHeader))
    }

    def 'Handle invalid Base64 encoding in X-Trans-Id header and add the whole string to the logging context'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingId = UUID.randomUUID().toString()
        def tracingHeader = "{{'tracingId': $tracingId, I_LIKE_HAM}".toString()
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader

        // logs
        reposeLogSearch.searchByString(Pattern.quote("Trans-Id:$tracingHeader"))
        reposeLogSearch.searchByString(Pattern.quote(tracingHeader))
    }

    def 'Handle legacy X-Trans-Id header (i.e. UUID string) and add the whole string to the logging context'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingHeader = UUID.randomUUID().toString()
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'

        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader
        mc.receivedResponse.headers.getFirstValue(CommonHttpHeader.TRACE_GUID) == tracingHeader

        // logs
        reposeLogSearch.searchByString(Pattern.quote("Trans-Id:$tracingHeader"))
        reposeLogSearch.searchByString(Pattern.quote(tracingHeader))
    }

    def 'Parse externally provided X-Trans-Id header and do not add the request id header'() {
        given:
        fakeIdentityService.with {
            client_tenantid = 1212
            client_userid = 1212
            client_token = UUID.randomUUID().toString()
            tokenExpiresAt = DateTime.now().plusDays(1)
        }

        def tracingId = UUID.randomUUID().toString()
        def sessionId = UUID.randomUUID().toString()
        def jsonTracingHeader = JsonOutput.toJson([sessionId: sessionId, requestId: tracingId, user: 'a', domain: 'b'])
        def tracingHeader = Base64.encodeBase64String(jsonTracingHeader.getBytes(Charset.forName("UTF-8")))
        def headers = [
                'content-type'                          : 'application/json',
                'X-Auth-Token'                          : fakeIdentityService.client_token,
                (CommonHttpHeader.TRACE_GUID): tracingHeader]

        when: 'User passes a request through repose'
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/1212",
                method: 'GET',
                headers: headers)

        then: 'Make sure the request and response contain a new X-Trans-Id header'
        mc.receivedResponse.code == '200'
        mc.handlings[0].request.headers.getFirstValue(CommonHttpHeader.REQUEST_ID) == null
    }
}
