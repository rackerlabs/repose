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
package features.filters.apivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.XmlParsing
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

/**
 * A test to verify that a user can validate authentication mechanisms via api-checker.
 */
@Category(XmlParsing)
class AuthenticatedByTest extends ReposeValveTest {

    static def AUTH_BY_HEADER = 'X-Authenticated-By'
    static def HTTP_METHODS = ['POST', 'GET', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS', 'TRACE']
    static def AUTH_BY_METHODS = ['PASSWORD', 'APIKEY', 'PASSCODE', 'OTPPASSCODE', 'IMPERSONATION', 'RSAKEY', 'FEDERATED']

    def authByHeader(String value) {
        [(AUTH_BY_HEADER): value]
    }

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/apivalidator/common', params)
        repose.configurationProvider.applyConfigs('features/filters/apivalidator/authenticatedBy', params)
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint)
    }

    @Unroll
    def 'A #method call to /v0/safe with the headers #headers should be Unauthorized.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/safe',
                method: method,
                headers: headers
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_UNAUTHORIZED

        where:
        [method, headers] << [
                HTTP_METHODS,
                AUTH_BY_METHODS.findAll{ it != 'PASSWORD' }.collect{ authByHeader(it) } + [null]
        ].combinations()
    }

    @Unroll
    def 'A #method call to /v0/safe with the headers [X-Authenticated-By: PASSWORD] should be Ok.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/safe',
                method: method,
                headers: ['X-Authenticated-By': 'PASSWORD']
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_OK

        where:
        method << HTTP_METHODS
    }

    @Unroll
    def 'A #method call to /v0/parent/inherited with the headers [X-Authenticated-By: APIKEY] should be Ok.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/parent/inherited',
                method: method,
                headers: ['X-Authenticated-By': 'APIKEY']
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_OK

        where:
        method << HTTP_METHODS
    }

    @Unroll
    def 'A #method call to /v0/#target with the headers #headers should be Ok.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + "/v0/$target",
                method: method,
                headers: headers
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_OK

        where:
        [target, method, headers] << [
                ['parent/open', 'none'],
                HTTP_METHODS,
                AUTH_BY_METHODS.collect{ authByHeader(it) } + [null]
        ].combinations()
    }

    @Unroll
    def 'A #method call to /v0/parent/child with the headers #headers should be Ok'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + "/v0/parent/child",
                method: method,
                headers: headers
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_OK

        where:
        [method, headers] << [
                HTTP_METHODS,
                ['APIKEY', 'RSAKEY', 'IMPERSONATION'].collect{ authByHeader(it) }
        ].combinations()
    }

    @Unroll
    def 'A #method call to /v0/vary with the headers #headers should be #responseCode.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/vary',
                method: method,
                headers: headers
        )

        then:
        messageChain.getReceivedResponse().getCode() == responseCode.toString()

        where:
        method   | headers                                    | responseCode
        'POST'   | null                                       | SC_UNAUTHORIZED
        'POST'   | [(AUTH_BY_HEADER): 'PASSWORD']             | SC_OK
        'POST'   | [(AUTH_BY_HEADER): 'FEDERATED']            | SC_OK
        'POST'   | [(AUTH_BY_HEADER): 'PASSWORD,FEDERATED']   | SC_OK
        'POST'   | [(AUTH_BY_HEADER): 'OTPPASSCODE,PASSWORD'] | SC_OK
        'POST'   | [(AUTH_BY_HEADER): 'PASSCODE,FEDERATED']   | SC_OK
        'POST'   | [(AUTH_BY_HEADER): 'PASSCODE']             | SC_UNAUTHORIZED
        'POST'   | [(AUTH_BY_HEADER): 'OTPPASSCODE']          | SC_UNAUTHORIZED
        'GET'    | null                                       | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'PASSWORD']             | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'APIKEY']               | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'PASSCODE']             | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'OTPPASSCODE']          | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'IMPERSONATION']        | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'RSAKEY']               | SC_OK
        'GET'    | [(AUTH_BY_HEADER): 'FEDERATED']            | SC_OK
        'PUT'    | null                                       | SC_UNAUTHORIZED
        'PUT'    | [(AUTH_BY_HEADER): 'PASSCODE']             | SC_OK
        'PUT'    | [(AUTH_BY_HEADER): 'PASSCODE,PASSCODE']    | SC_OK
        'PUT'    | [(AUTH_BY_HEADER): 'FEDERATED']            | SC_UNAUTHORIZED
        'PUT'    | [(AUTH_BY_HEADER): 'APIKEY']               | SC_UNAUTHORIZED
        'PATCH'  | null                                       | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'PASSWORD']             | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'APIKEY']               | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'PASSCODE']             | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'OTPPASSCODE']          | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'IMPERSONATION']        | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'RSAKEY']               | SC_OK
        'PATCH'  | [(AUTH_BY_HEADER): 'FEDERATED']            | SC_OK
        'DELETE' | null                                       | SC_UNAUTHORIZED
        'DELETE' | [(AUTH_BY_HEADER): 'OTPPASSCODE']          | SC_OK
        'DELETE' | [(AUTH_BY_HEADER): 'OTPPASSCODE,PASSCODE'] | SC_OK
        'DELETE' | [(AUTH_BY_HEADER): 'RSAKEY']               | SC_UNAUTHORIZED
        'DELETE' | [(AUTH_BY_HEADER): 'APIKEY']               | SC_UNAUTHORIZED
        'DELETE' | [(AUTH_BY_HEADER): 'IMPERSONATION']        | SC_UNAUTHORIZED
    }

    @Unroll
    def 'A #method call to /v0/vary with the headers #headers should be Not Allowed.'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
                url: reposeEndpoint + '/v0/vary',
                method: method,
                headers: headers
        )

        then:
        messageChain.getReceivedResponse().getCode() as Integer == SC_METHOD_NOT_ALLOWED

        where:
        [method, headers] << [
                ['HEAD', 'OPTIONS', 'TRACE'],
                AUTH_BY_METHODS.collect{ authByHeader(it) } + [null]
        ].combinations()
    }
}
