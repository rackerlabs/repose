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
import org.rackspace.deproxy.MessageChain

import static features.filters.samlpolicy.util.SamlPayloads.*
import static features.filters.samlpolicy.util.SamlUtilities.*
import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED

/**
 * This functional test verifies the filter is setting the content length of the request correctly for downstream
 * filters to consume.
 */
class SamlContentLengthTest extends ReposeValveTest {
    final static String SCRIPT_LOG_CLASS = "features.filters.samlpolicy.SamlContentLengthTest.script"
    final static String NO_HEADER_VALUE = "%NO_HEADER_VALUE%"
    final static String HEADER_CAPTURE_PREPEND = "SamlContentLengthTest-"
    final static List<String> HEADERS_TO_CAPTURE = ["'$CONTENT_LENGTH'", "'$TRANSFER_ENCODING'"]

    static MockIdentityV2Service fakeIdentityV2Service

    def setupSpec() {
        reposeLogSearch.cleanLog()

        def params = properties.defaultTemplateParams + [
                scriptLogClass: SCRIPT_LOG_CLASS,
                noHeaderValue: NO_HEADER_VALUE,
                headerCapturePrepend: HEADER_CAPTURE_PREPEND,
                headersToCapture: HEADERS_TO_CAPTURE as String]
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/flow1_0", params)
        repose.configurationProvider.applyConfigs("features/filters/samlpolicy/contentlength", params)

        deproxy = new Deproxy()

        fakeIdentityV2Service = new MockIdentityV2Service(properties.identityPort, properties.targetPort)
        deproxy.addEndpoint(properties.targetPort, 'origin service', null, fakeIdentityV2Service.handler)
        deproxy.addEndpoint(properties.identityPort, 'identity service', null, fakeIdentityV2Service.handler)

        repose.start()
        reposeLogSearch.awaitByString("Repose ready", 1, 30)

        fakeIdentityV2Service.admin_token = UUID.randomUUID().toString()
    }

    def "for a saml:response with a legacy Issuer, the filter will update the request to have a correct content-length/transfer-encoding header"() {
        given: "a saml:response with a legacy Issuer"
        def saml = samlResponse(issuer(SAML_LEGACY_ISSUER) >> status() >> assertion(issuer: SAML_LEGACY_ISSUER))

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the request makes it to the origin service"
        mc.handlings[0]

        and: "either the filter set the content-length header correctly or it used chunked encoding"
        (contentLengthSetCorrectly(mc) && transferEncodingNotSet(mc)) || (chunkedEncodingSet(mc) && contentLengthNotSet(mc))
    }

    def "for a saml:response with a non-legacy Issuer, the filter will update the request to have a correct content-length/transfer-encoding header"() {
        given: "a saml:response with a unique non-legacy Issuer"
        def samlIssuer = generateUniqueIssuer()
        def saml = samlResponse(issuer(samlIssuer) >> status() >> assertion(issuer: samlIssuer, fakeSign: true))

        when: "a request is sent to Repose"
        def mc = deproxy.makeRequest(
                url: reposeEndpoint + SAML_AUTH_URL,
                method: HTTP_POST,
                headers: [(CONTENT_TYPE): APPLICATION_FORM_URLENCODED],
                requestBody: asUrlEncodedForm((PARAM_SAML_RESPONSE): encodeBase64(saml)))

        then: "the request makes it to the origin service"
        mc.handlings[0]

        and: "either the filter set the content-length header correctly or it used chunked encoding"
        (contentLengthSetCorrectly(mc) && transferEncodingNotSet(mc)) || (chunkedEncodingSet(mc) && contentLengthNotSet(mc))
    }

    def contentLengthSetCorrectly(MessageChain mc) {
        getContentLengthSetByFilter(mc) == mc.handlings[0].request.headers.getFirstValue(CONTENT_LENGTH)
    }

    def chunkedEncodingSet(MessageChain mc) {
        getTransferEncodingSetByFilter(mc) == CHUNKED_ENCODING
    }

    def contentLengthNotSet(MessageChain mc) {
        getContentLengthSetByFilter(mc) == NO_HEADER_VALUE
    }

    def transferEncodingNotSet(MessageChain mc) {
        getTransferEncodingSetByFilter(mc) == NO_HEADER_VALUE
    }

    def getContentLengthSetByFilter(MessageChain mc) {
        mc.handlings[0].request.headers.getFirstValue(HEADER_CAPTURE_PREPEND + CONTENT_LENGTH)
    }

    def getTransferEncodingSetByFilter(MessageChain mc) {
        mc.handlings[0].request.headers.getFirstValue(HEADER_CAPTURE_PREPEND + TRANSFER_ENCODING)
    }
}
