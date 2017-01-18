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

import org.opensaml.saml.saml2.core.Response
import spock.lang.Specification
import spock.lang.Unroll

import static features.filters.samlpolicy.SamlPayloads.*
import static features.filters.samlpolicy.SamlUtilities.*

/**
 * Who tests the test code?
 */
class SamlUtilitiesSelfTest extends Specification {

    static samlUtilities = new SamlUtilities()

    @Unroll
    def "SAML Utility can generate a valid SAML Response #testDescription"() {
        when: "we unmarshall the SAML string and try to validate the first Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the signature was valid"
        isValidSignature

        and: "the Issuer was set correctly"
        response.issuer.value == "http://idp.external.com"

        where:
        [testDescription, saml] << [
                ["using MarkupBuilder, a custom closure, and a known good assertion string", samlResponse {
                    'saml2:Issuer'("http://idp.external.com")
                    'saml2p:Status' {
                        'saml2p:StatusCode'(Value: "urn:oasis:names:tc:SAML:2.0:status:Success")
                    }
                    mkp.yieldUnescaped ASSERTION_SIGNED
                }],
                ["using MarkupBuilder and closure composition", samlResponse(issuer() >> status() >> assertion())],
                ["using a known good payload", SAML_ONE_ASSERTION_SIGNED]
        ]
    }

    @Unroll
    def "SAML Utility validator will reject a SAML response with an invalid signature #testDescription"() {
        when: "we unmarshall the SAML string and try to validate the Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature)

        then: "the signature was not valid"
        !isValidSignature

        and: "the Issuer was set correctly"
        response.issuer.value == "http://idp.external.com"

        where:
        saml                                          | testDescription
        SAML_ONE_ASSERTION_SIGNED.replace("    ", "") | "by tampering with the whitespace of a valid payload"
        SAML_ASSERTION_INVALID_SIGNATURE              | "using a known bad payload"
    }
}
