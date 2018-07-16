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
package features.core.intrafilterlogging

import groovy.json.JsonSlurper
import org.openrepose.framework.test.ReposeValveTest
import org.openrepose.framework.test.mocks.MockIdentityV2Service
import org.openrepose.framework.test.mocks.MockIdentityV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders

import static javax.servlet.http.HttpServletResponse.SC_OK

class IntraFilterLoggingTest extends ReposeValveTest {
    def static originEndpoint
    def static keystoneV2Endpoint
    def static openstackV3Endpoint
    static MockIdentityV2Service fakeIdentityService
    static MockIdentityV3Service fakeOpenstackService
    static String content = 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac malesuada. Integer nec est turpis duis.'
    def static logPreStringRequest = 'TRACE intrafilter-logging.*Intrafilter Request Log.*"currentFilter":"'
    def static logPreStringResponse = 'TRACE intrafilter-logging.*Intrafilter Response Log.*"currentFilter":"'
    def static configuredFilters = [
        'keystone-v2-basic-auth',
        'add-header',
        'keystone-v2',
        'openstack-identity-v3',
        'ip-user',
        'content-type-stripper',
        'header-user',
        'header-normalization',
        'header-translation',
        'merge-header',
        'slf4j-http-logging',
        'uri-user',
        'uri-normalization',
        'uri-stripper',
        'compression',
        'rate-limiting',
        'simple-rbac',
        'api-validator',
        'herp',
        'derp',
    ]
    def static logPostStringAny = '".*'

    def setup() {
        reposeLogSearch.cleanLog()
    }

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.cleanConfigDirectory()
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/core/intrafilterlogging', params)

        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        fakeIdentityService.checkTokenValid = true
        keystoneV2Endpoint = deproxy.addEndpoint(properties.identityPort, 'identity v2 service', null, fakeIdentityService.handler)
        fakeOpenstackService = new MockIdentityV3Service(properties.identityPort2, properties.targetPort)
        openstackV3Endpoint = deproxy.addEndpoint(properties.identityPort2, 'identity v3 service', null, fakeOpenstackService.handler)

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def "Verify intra filter log for current filter no longer have 'null' in the description (#size)"() {
        given: "a unique token"
        def client_token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = client_token
        fakeOpenstackService.client_token = client_token
        def headers = [
            'x-roles'        : 'raxRolesDisabled',
            'X-Auth-Token'   : client_token,
            'X-Subject-Token': client_token,
        ]

        when: "a request request with credentials is sent"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/test",
            method: 'GET',
            headers: headers + addHeader)

        then: "return with an OK (200) and it should reach the origin service"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1

        and: "log at every filter on the request"
        configuredFilters.each { filterName ->
            def logSearch = reposeLogSearch.searchByString("$logPreStringRequest$filterName$logPostStringAny")
            assert logSearch.size() == size
            if (size > 0) {
                def json = convertToJson(logSearch[0])
                assertHeadersExists(['X-Auth-Token', 'Intrafilter-UUID'], json)
                assertKeyValueMatch([
                    'currentFilter': filterName,
                    'httpMethod'   : 'GET',
                    'requestURI'   : '/test',
                    'requestBody'  : '',
                ], json)
            }
        }
        and: "log at every filter on the response"
        configuredFilters.each { filterName ->
            def logSearch = reposeLogSearch.searchByString("$logPreStringResponse$filterName$logPostStringAny")
            assert logSearch.size() == size
            if (size > 0) {
                def json = convertToJson(logSearch[0])
                assertHeadersExists(['Intrafilter-UUID'], json)
                assertKeyValueMatch([
                    'currentFilter'   : filterName,
                    'responseBody'    : '',
                    'httpResponseCode': SC_OK as String,
                ], json)
            }
        }
        and: "add the headers to the response"
        configuredFilters.each { filterName ->
            assert mc.receivedResponse.headers.findAll("X-$filterName-Time").size() == size
        }
        and: "not log at any filter a NULL in the name"
        configuredFilters.each { filterName ->
            assert reposeLogSearch.searchByString("null-$filterName").size() == 0
        }

        where:
        addHeader                   || size
        []                          || 0
        ['x-trace-request': 'true'] || 1
    }

    @Unroll
    def "verify origin receives request body from client and that it is properly logged (#size)"() {
        given: "a unique token"
        def client_token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = client_token
        fakeOpenstackService.client_token = client_token
        def headers = [
            'x-roles'        : 'raxRolesDisabled',
            'X-Auth-Token'   : client_token,
            'X-Subject-Token': client_token,
        ]

        when: "a request request with credentials and body content is sent"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/test",
            method: 'POST',
            headers: headers + addHeader,
            requestBody: content)

        then: "return with an OK (200) and it should reach the origin service"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1

        and: "origin service should receive the full body"
        mc.sentRequest.body == content

        and: "log at every filter on the request"
        configuredFilters.each { filterName ->
            def logSearch = reposeLogSearch.searchByString("$logPreStringRequest$filterName$logPostStringAny")
            assert logSearch.size() == size
            if (size > 0) {
                def json = convertToJson(logSearch[0])
                assertKeyValueMatch([
                    'currentFilter': filterName,
                    'httpMethod'   : 'POST',
                    'requestURI'   : '/test',
                    'requestBody'  : content,
                ], json)
            }
        }

        where:
        addHeader                   || size
        []                          || 0
        ['X-Trace-Request': 'true'] || 1
    }

    @Unroll
    def "verify client gets the response body from the origin and that it is properly logged (#size)"() {
        given: "a unique token"
        def client_token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = client_token
        fakeOpenstackService.client_token = client_token
        def headers = [
            'x-roles'        : 'raxRolesDisabled',
            'X-Auth-Token'   : client_token,
            'X-Subject-Token': client_token,
        ]

        when: "a request request with credentials is sent"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/test",
            method: 'GET',
            headers: headers + addHeader,
            defaultHandler: { new Response(SC_OK, 'OK', [], content) })

        then: "return with an OK (200) and it should reach the origin service"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1

        and: "log at every filter on the response"
        configuredFilters.each { filterName ->
            def logSearch = reposeLogSearch.searchByString("$logPreStringResponse$filterName$logPostStringAny")
            assert logSearch.size() == size
            if (size > 0) {
                def json = convertToJson(logSearch[0])
                assertKeyValueMatch([
                    'currentFilter'   : filterName,
                    'responseBody'    : content,
                    'httpResponseCode': SC_OK as String,
                ], json)
            }
        }

        and: "client should receive the full body"
        mc.receivedResponse.body == content

        where:
        addHeader                   || size
        []                          || 0
        ['x-tRACE-rEQUEST': 'true'] || 1
    }

    def "ensure that intrafilter logging isn't munching the x-pp-user headers"() {
        given: "a unique token"
        def client_token = UUID.randomUUID().toString()
        fakeIdentityService.client_token = client_token
        fakeOpenstackService.client_token = client_token
        def headers = [
            'x-roles'        : 'raxRolesDisabled',
            'X-Auth-Token'   : client_token,
            'X-Subject-Token': client_token,
            'x-pp-user'      : 'Developers;q=1.0 , Secure Developers;q=0.9 , service:admin-role1 , member',
            'X-TRACE-REQUEST': 'true',
        ]

        when: "a request request with multiple x-pp-user headers"
        MessageChain mc = deproxy.makeRequest(
            url: "$reposeEndpoint/test",
            method: 'GET',
            headers: headers,
            defaultHandler: {
                new Response(
                    SC_OK,
                    'OK',
                    [
                        (HttpHeaders.CONTENT_TYPE): 'application/json',
                        'x-pp-user'               : 'one,two,three',
                    ],
                    /"{"response": "amazing"}"/)
            })

        then: "return with an OK (200) and it should reach the origin service"
        mc.receivedResponse.code as Integer == SC_OK
        mc.handlings.size() == 1

        and: "make sure that we didn't clobber the x-pp-user header entries in the log output"
        def jsonReq = convertToJson(reposeLogSearch.searchByString(logPreStringRequest)[0])
        jsonReq.headers.'x-pp-user'.split(',').size() == 4

        and: "verify the response while we're at it"
        def jsonRes = convertToJson(reposeLogSearch.searchByString(logPreStringResponse)[0])
        jsonRes.headers.'x-pp-user'.split(',').size() == 3
    }

    private static convertToJson(String searchString) {
        def searchJson = searchString.substring(searchString.indexOf('{"preamble"'))
        println searchJson
        def slurper = new JsonSlurper()
        slurper.parseText(searchJson)
    }

    private static assertHeadersExists(List headers, Object jsonObject) {
        Set<String> headerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER)
        headerNames.addAll(jsonObject.headers.keySet())
        headers.each {
            headerName ->
                assert headerNames.contains(headerName)
        }
    }

    private static assertKeyValueMatch(Map headers, Object jsonObject) {
        headers.each {
            key, value ->
                assert jsonObject."$key" == value
        }
    }
}
