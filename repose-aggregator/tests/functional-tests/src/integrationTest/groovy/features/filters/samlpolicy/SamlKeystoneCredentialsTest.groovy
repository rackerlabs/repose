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

package features.filters.samlpolicy

import framework.ReposeValveTest
import framework.mocks.MockIdentityV2Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static framework.mocks.MockIdentityV2Service.createIdentityFaultJsonWithValues
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import static javax.servlet.http.HttpServletResponse.SC_OK
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED
import static javax.ws.rs.core.MediaType.APPLICATION_JSON

/**
 * This functional test exercises the keystone credentials logic.
 */
class SamlKeystoneCredentialsTest extends ReposeValveTest {

    static MockIdentityV2Service fakeIdentityV2Service

    def setup() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)

        deproxy = new Deproxy()

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
        fakeIdentityV2Service.resetCounts()
        fakeIdentityV2Service.resetHandlers()
    }

    def cleanup() {
        deproxy?.shutdown()
        repose?.stop()
    }

    def "the admin token will only be generated once (as long as it remains valid)"() {
        when: "a request is sent to Repose"
        def mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was generated"
        fakeIdentityV2Service.getGenerateTokenCount() == 1

        when: "another request is sent to Repose"
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was not generated again"
        fakeIdentityV2Service.getGenerateTokenCount() == 0
    }

    def "the admin token will be generated again when the issuer call returns a 401"() {
        when: "a request is sent to Repose"
        def mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was generated"
        fakeIdentityV2Service.getGenerateTokenCount() == 1

        when: "the admin token in the mock changes which will result in a 401 being returned from the issuer call"
        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()

        and: "another request is sent to Repose"
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was generated again"
        fakeIdentityV2Service.getGenerateTokenCount() == 1

        and: "the issuer endpoint was called twice (first time resulting in a 401, second time being successful)"
        fakeIdentityV2Service.getIdpFromIssuerCount() == 2
    }

    def "the admin token will be generated again when the mapping policy call returns a 401"() {
        given: "the mock issuer call will skip the auth check"
        fakeIdentityV2Service.getIdpFromIssuerHandler = fakeIdentityV2Service.createGetIdpFromIssuerHandler(skipAuthCheck: true)

        when: "a request is sent to Repose"
        def mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was generated"
        fakeIdentityV2Service.getGenerateTokenCount() == 1

        when: "the admin token in the mock changes which will result in a 401 being returned from the mapping policy call"
        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()

        and: "another request is sent to Repose"
        fakeIdentityV2Service.resetCounts()
        mc = sendSamlRequest()

        then: "the origin service receives the request and the client receives the response"
        mc.handlings[0]
        mc.receivedResponse.code as Integer == SC_OK

        and: "the admin token was generated again"
        fakeIdentityV2Service.getGenerateTokenCount() == 1

        and: "the mapping policy endpoint was called twice (first time resulting in a 401, second time being successful)"
        fakeIdentityV2Service.getMappingPolicyForIdpCount == 2
    }

    def "Repose will return a 500 if the configured keystone credentials don't have the necessary roles for the IDP endpoints"() {
        given: "the Identity mock will return a 403 indicating the provided token doesn't have the necessary roles for the endpoint"
        fakeIdentityV2Service.getIdpFromIssuerHandler = { String issuer, Request request ->
            new Response(
                    SC_FORBIDDEN,
                    null,
                    [(CONTENT_TYPE): APPLICATION_JSON],
                    createIdentityFaultJsonWithValues(
                            name: "forbidden",
                            code: SC_FORBIDDEN,
                            message: "Not Authorized"))
        }

        when: "a request is sent to Repose"
        def mc = sendSamlRequest()

        then: "the request does not get to the origin service"
        mc.handlings.isEmpty()

        and: "the client gets a 500"
        mc.receivedResponse.code as Integer == SC_INTERNAL_SERVER_ERROR
    }

    def sendSamlRequest() {
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))
    }
}
