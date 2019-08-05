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
package features.filters.openapivalidator

import org.junit.experimental.categories.Category
import org.openrepose.framework.test.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import scaffold.category.Filters
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_OK

/**
 * Verifies the behavior of the OpenAPI Validator Filter.
 */
@Category(Filters)
class OpenApiValidationTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs('common', params)
        repose.configurationProvider.applyConfigs('features/filters/openapivalidation/validation', params)
        repose.start()
    }

    @Unroll
    def 'when configured with a v#version #format document, should validate a simple request'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v$version/$format/pets"
        )

        then:
        messageChain.receivedResponse.code as Integer == 200
        verifyHandlings(messageChain)

        where:
        [version, format] << [['2', '3'], ['json', 'yaml']].combinations()
    }

    @Unroll
    def 'when configured with a document at #documentHref, should validate a simple request'() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v3/$documentHref/pets"
        )

        then:
        messageChain.receivedResponse.code as Integer == 200
        verifyHandlings(messageChain)

        where:
        documentHref << ['absolute', 'absoluteFile']
    }

    @Unroll
    def "when configured to validate a path, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v3/validations/$path"
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        path          || expectedResponseStatus | description
        'unspecified' || 404                    | 'reject a request for an unspecified path'
        '@'           || 404                    | 'reject a request for an invalid path'
        'path'        || 200                    | 'pass a request for a specified path'
    }

    @Unroll
    def "when configured to validate a query parameter, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v3/validations/query?$queryString"
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        queryString || expectedResponseStatus | description
        'foo=bar'   || 400                    | 'reject a request without the query parameter'
        'int=foo'   || 400                    | 'reject a request with the query parameter with an invalid value'
        'int=32'    || 200                    | 'pass a request with the query parameter with a valid value'
    }

    @Unroll
    def "when configured to validate a method, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: method,
            url: reposeEndpoint + "/v3/validations/method"
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        method || expectedResponseStatus | description
        'PUT'  || 405                    | 'reject a request for an unsupported operation'
        'GET'  || 200                    | 'pass a request for a supported operation'
    }

    @Unroll
    def "when configured to validate a required header, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v3/validations/header",
            headers: headers
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        headers                        || expectedResponseStatus | description
        [:]                            || 400                    | 'reject a request without the header'
        ['x-int-header': 'not an int'] || 400                    | 'reject a request with the header with an invalid value'
        ['x-int-header': '32']         || 200                    | 'pass a request with the header with a valid value'
    }

    @Unroll
    def "Should receive #expectedResponseStatus when the rax-role #raxRole is present on a #method to #path)"() {
        when:
        MessageChain messageChain1 = deproxy.makeRequest(
            method: 'GET',
            url: "$reposeEndpoint/v3/validations/rax-roles/$path",
            headers: ['x-rax-roles': raxRole]
        )

        then:
        messageChain1.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain1)

        when:
        MessageChain messageChain2 = deproxy.makeRequest(
            method: 'GET',
            url: "$reposeEndpoint/v3/validations/rax-roles/$path",
            headers: ['x-rax-roles': "Bogus, $raxRole, Stuff"]
        )

        then:
        messageChain2.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain2)

        where:
        path      | method   | raxRole          || expectedResponseStatus
        // Multi
        'multi/a' | 'GET'    | 'admin'          || SC_OK
        'multi/a' | 'PUT'    | 'admin'          || SC_OK
        'multi/a' | 'POST'   | 'admin'          || SC_OK
        'multi/a' | 'DELETE' | 'admin'          || SC_OK
        'multi/a' | 'GET'    | 'roleGET'        || SC_OK
        'multi/a' | 'PUT'    | 'rolePUT'        || SC_OK
        'multi/a' | 'POST'   | 'rolePOST'       || SC_OK
        'multi/a' | 'DELETE' | 'roleDELETE'     || SC_OK
        'multi/a' | 'GET'    | 'nimda'          || SC_BAD_REQUEST
        'multi/a' | 'PUT'    | 'nimda'          || SC_BAD_REQUEST
        'multi/a' | 'POST'   | 'nimda'          || SC_BAD_REQUEST
        'multi/a' | 'DELETE' | 'nimda'          || SC_BAD_REQUEST
        'multi/a' | 'GET'    | 'roleBAD'        || SC_BAD_REQUEST
        'multi/a' | 'PUT'    | 'roleBAD'        || SC_BAD_REQUEST
        'multi/a' | 'POST'   | 'roleBAD'        || SC_BAD_REQUEST
        'multi/a' | 'DELETE' | 'roleBAD'        || SC_BAD_REQUEST
        'multi/a' | 'GET'    | 'roleGOOD'       || SC_BAD_REQUEST
        'multi/a' | 'PUT'    | 'roleGOOD'       || SC_BAD_REQUEST
        'multi/a' | 'POST'   | 'roleGOOD'       || SC_BAD_REQUEST
        'multi/a' | 'DELETE' | 'roleGOOD'       || SC_BAD_REQUEST
        'multi/b' | 'GET'    | 'nimda'          || SC_OK
        'multi/b' | 'PUT'    | 'nimda'          || SC_OK
        'multi/b' | 'POST'   | 'nimda'          || SC_OK
        'multi/b' | 'DELETE' | 'nimda'          || SC_OK
        'multi/b' | 'GET'    | 'roleGOOD'       || SC_OK
        'multi/b' | 'PUT'    | 'roleGOOD'       || SC_OK
        'multi/b' | 'POST'   | 'roleGOOD'       || SC_OK
        'multi/b' | 'DELETE' | 'roleGOOD'       || SC_OK
        'multi/b' | 'GET'    | 'admin'          || SC_BAD_REQUEST
        'multi/b' | 'PUT'    | 'admin'          || SC_BAD_REQUEST
        'multi/b' | 'POST'   | 'admin'          || SC_BAD_REQUEST
        'multi/b' | 'DELETE' | 'admin'          || SC_BAD_REQUEST
        'multi/b' | 'GET'    | 'roleGET'        || SC_BAD_REQUEST
        'multi/b' | 'PUT'    | 'rolePUT'        || SC_BAD_REQUEST
        'multi/b' | 'POST'   | 'rolePOST'       || SC_BAD_REQUEST
        'multi/b' | 'DELETE' | 'roleDELETE'     || SC_BAD_REQUEST
        // Dash
        'dash/a'  | 'GET'    | 'This-Is-A-Role' || SC_OK
        'dash/a'  | 'GET'    | 'role-GET'       || SC_OK
        'dash/a'  | 'PUT'    | 'role-PUT'       || SC_OK
        'dash/a'  | 'POST'   | 'role-POST'      || SC_OK
        'dash/a'  | 'DELETE' | 'role-DELETE'    || SC_OK
        'dash/a'  | 'GET'    | 'role-BAD'       || SC_BAD_REQUEST
        'dash/a'  | 'PUT'    | 'role-BAD'       || SC_BAD_REQUEST
        'dash/a'  | 'POST'   | 'role-BAD'       || SC_BAD_REQUEST
        'dash/a'  | 'DELETE' | 'role-BAD'       || SC_BAD_REQUEST
        'dash/a'  | 'GET'    | 'role-GOOD'      || SC_BAD_REQUEST
        'dash/a'  | 'PUT'    | 'role-GOOD'      || SC_BAD_REQUEST
        'dash/a'  | 'POST'   | 'role-GOOD'      || SC_BAD_REQUEST
        'dash/a'  | 'DELETE' | 'role-GOOD'      || SC_BAD_REQUEST
        'dash/b'  | 'GET'    | 'role-GOOD'      || SC_OK
        'dash/b'  | 'PUT'    | 'role-GOOD'      || SC_OK
        'dash/b'  | 'POST'   | 'role-GOOD'      || SC_OK
        'dash/b'  | 'DELETE' | 'role-GOOD'      || SC_OK
        'dash/b'  | 'GET'    | 'This-Is-A-Role' || SC_BAD_REQUEST
        'dash/b'  | 'GET'    | 'role-GET'       || SC_BAD_REQUEST
        'dash/b'  | 'PUT'    | 'role-PUT'       || SC_BAD_REQUEST
        'dash/b'  | 'POST'   | 'role-POST'      || SC_BAD_REQUEST
        'dash/b'  | 'DELETE' | 'role-DELETE'    || SC_BAD_REQUEST
        // Space
        'space/a' | 'GET'    | 'This Is A Role' || SC_OK
        'space/a' | 'GET'    | 'role GET'       || SC_OK
        'space/a' | 'PUT'    | 'role PUT'       || SC_OK
        'space/a' | 'POST'   | 'role POST'      || SC_OK
        'space/a' | 'DELETE' | 'role DELETE'    || SC_OK
        'space/a' | 'GET'    | 'role BAD'       || SC_BAD_REQUEST
        'space/a' | 'PUT'    | 'role BAD'       || SC_BAD_REQUEST
        'space/a' | 'POST'   | 'role BAD'       || SC_BAD_REQUEST
        'space/a' | 'DELETE' | 'role BAD'       || SC_BAD_REQUEST
        'space/a' | 'GET'    | 'role GOOD'      || SC_BAD_REQUEST
        'space/a' | 'PUT'    | 'role GOOD'      || SC_BAD_REQUEST
        'space/a' | 'POST'   | 'role GOOD'      || SC_BAD_REQUEST
        'space/a' | 'DELETE' | 'role GOOD'      || SC_BAD_REQUEST
        'space/b' | 'GET'    | 'role GOOD'      || SC_OK
        'space/b' | 'PUT'    | 'role GOOD'      || SC_OK
        'space/b' | 'POST'   | 'role GOOD'      || SC_OK
        'space/b' | 'DELETE' | 'role GOOD'      || SC_OK
        'space/b' | 'GET'    | 'This Is A Role' || SC_BAD_REQUEST
        'space/b' | 'GET'    | 'role GET'       || SC_BAD_REQUEST
        'space/b' | 'PUT'    | 'role PUT'       || SC_BAD_REQUEST
        'space/b' | 'POST'   | 'role POST'      || SC_BAD_REQUEST
        'space/b' | 'DELETE' | 'role DELETE'    || SC_BAD_REQUEST
    }

    @Unroll
    def "when configured to validate the Accept media type, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'GET',
            url: reposeEndpoint + "/v3/validations/accept",
            headers: accept
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        accept                       || expectedResponseStatus | description
        [Accept: 'foo']              || 400                    | 'reject a request for an invalid accept media type'
        [Accept: 'application/xml']  || 406                    | 'reject a request for an unsupported accept media type'
        [Accept: 'application/json'] || 200                    | 'pass a request for the supported accept media type'
        [Accept: '*/*']              || 200                    | 'pass a request for a supported accept media type'
        [:]                          || 200                    | 'pass a request without a specified accept media type'
    }

    @Unroll
    def "when configured to validate the Content-Type media type, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'POST',
            url: reposeEndpoint + "/v3/validations/contentType",
            headers: contentType + ['Content-Length': body.length()],
            requestBody: body
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        contentType                          | body          || expectedResponseStatus | description
        [:]                                  | 'lorem ipsum' || 415                    | 'reject a request without a specified content type media type'
        ['Content-Type': 'foo']              | 'foo'         || 400                    | 'reject a request with an invalid content type media type'
        ['Content-Type': 'application/xml']  | '<root/>'     || 415                    | 'reject a request with an unsupported content type media type'
        ['Content-Type': 'application/json'] | '{}'          || 200                    | 'pass a request with the supported content type media type'
    }

    @Unroll
    def "when configured to validate the request body, should #description"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'POST',
            url: reposeEndpoint + "/v3/validations/requestBody",
            headers: ['Content-Type': 'application/json', 'Content-Length': body.length()],
            requestBody: body
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        verifyHandlings(messageChain)

        where:
        body                         || expectedResponseStatus | description
        ''                           || 400                    | 'reject a request without a body'
        '{ foo: "bar" }'             || 400                    | 'reject a request with an invalid JSON body'
        '{ "testKey": "testValue" }' || 200                    | 'pass a request with a valid JSON body'
    }

    def "when configured to validate a chunked request body, should pass a request with a valid JSON body"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'POST',
            url: reposeEndpoint + "/v3/validations/requestBody",
            headers: ['Content-Type': 'application/json'],
            requestBody: '{ "testKey": "testValue" }',
            chunked: true
        )

        then:
        messageChain.receivedResponse.code as Integer == 200
        verifyHandlings(messageChain)
    }

    @Unroll
    def "when configured to validate multiple components of the request, should #description according to priority"() {
        when:
        MessageChain messageChain = deproxy.makeRequest(
            method: 'POST',
            url: reposeEndpoint + "/v3/validations/multiple" + queryString,
            headers: ['Content-Type': 'application/json', 'Content-Length': body.length()] + headers,
            requestBody: body
        )

        then:
        messageChain.receivedResponse.code as Integer == expectedResponseStatus
        messageChain.receivedResponse.message =~ expectedReasonFragment
        verifyHandlings(messageChain)

        where:
        headers                | queryString | body || expectedResponseStatus | expectedReasonFragment | description
        [:]                    | '?int=32'   | '{}' || 400                    | 'Header'               | 'reject a request without a required header'
        ['x-int-header': '32'] | ''          | '{}' || 400                    | 'Query parameter'      | 'reject a request without a required query parameter'
        ['x-int-header': '32'] | '?int=32'   | ''   || 400                    | 'request body'         | 'reject a request without a required body'
        [:]                    | ''          | '{}' || 400                    | 'Header'               | 'reject a request without a required header and query parameter'
        [:]                    | '?int=32'   | ''   || 400                    | 'request body'         | 'reject a request without a required header and body'
        ['x-int-header': '32'] | ''          | ''   || 400                    | 'request body'         | 'reject a request without a required query parameter and body'
        ['x-int-header': '32'] | '?int=32'   | '{}' || 200                    | 'OK'                   | 'pass a request satisfying all criteria'
    }

    static void verifyHandlings(MessageChain messageChain) {
        int statusCode = messageChain.receivedResponse.code as Integer
        if (200 <= statusCode && statusCode < 300) {
            assert messageChain.handlings.size() == 1
        } else {
            assert messageChain.handlings.empty
        }
    }
}
