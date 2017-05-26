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

package org.openrepose.framework.test.util.saml

import groovy.xml.MarkupBuilder
import org.opensaml.saml.saml2.core.Response
import spock.lang.Specification
import spock.lang.Unroll

import static org.openrepose.framework.test.util.saml.SamlPayloads.*
import static org.openrepose.framework.test.util.saml.SamlUtilities.*

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
        response.issuer.value == SAML_EXTERNAL_ISSUER

        where:
        [testDescription, saml] << [
            ["using MarkupBuilder, a custom closure, and a known good assertion string",
             samlResponse { MarkupBuilder builder ->
                 builder.'saml2:Issuer'(SAML_EXTERNAL_ISSUER)
                 builder.'saml2p:Status' {
                     'saml2p:StatusCode'(Value: SAML_STATUS_SUCCESS)
                 }
                 builder.mkp.yieldUnescaped ASSERTION_SIGNED
                 builder
             }],
            ["using MarkupBuilder and closure composition", samlResponse(issuer() >> status() >> assertion())],
            ["using a known good payload", SAML_ONE_ASSERTION_SIGNED]
        ]
    }

    @Unroll
    def "the Assertion signature of the SAML payload '#payloadDescription' is deemed valid '#shouldBeValid' when checked against the cert entity id '#certificateEntityId'"() {
        when: "we unmarshall the SAML string and try to validate the first Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature, certificateEntityId)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the signature's validity matches expectations"
        isValidSignature == shouldBeValid

        where:
        saml                                                        | certificateEntityId       | shouldBeValid | payloadDescription
        SAML_ONE_ASSERTION_SIGNED                                   | "idp.external.com"        | true          | "known good payload"
        SAML_ASSERTION_AND_MESSAGE_SIGNED                           | "idp.external.com"        | true          | "known good payload with message signed too"
        SAML_LEGACY_ISSUER_SIGNED_ASSERTION                         | "legacy.idp.external.com" | true          | "known good payload with legacy issuer"
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE_AND_ASSERTION             | "legacy.idp.external.com" | true          | "known good payload with message signed too and a legacy issuer"
        SAML_ONE_ASSERTION_SIGNED                                   | "not.the.idp.com"         | false         | "known good payload"
        SAML_LEGACY_ISSUER_SIGNED_ASSERTION                         | "not.the.idp.com"         | false         | "known good payload with legacy issuer"
        SAML_ONE_ASSERTION_SIGNED.replace("    ", "")               | "idp.external.com"        | false         | "altered known good payload"
        SAML_LEGACY_ISSUER_SIGNED_ASSERTION.replace("    ", "")     | "legacy.idp.external.com" | false         | "altered known good payload with legacy issuer"
        SAML_ASSERTION_INVALID_SIGNATURE                            | "idp.external.com"        | false         | "known bad payload"
        samlResponse(issuer() >> assertion(ASSERTION_SIGNED))       | "idp.external.com"        | true          | "generated saml with signature payload 1"
        samlResponse(issuer() >> assertion(ASSERTION_SIGNED_TWO))   | "idp.external.com"        | true          | "generated saml with signature payload 2"
        samlResponse(issuer() >> assertion(ASSERTION_SIGNED_THREE)) | "idp.external.com"        | true          | "generated saml with signature payload 3"
    }

    @Unroll
    def "the Response signature of the SAML payload '#payloadDescription' is deemed valid '#shouldBeValid' when checked against the cert entity id '#certificateEntityId'"() {
        when: "we unmarshall the SAML string and try to validate the first Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.signature, certificateEntityId)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the signature's validity matches expectations"
        isValidSignature == shouldBeValid

        where:
        saml                                                                | certificateEntityId       | shouldBeValid | payloadDescription
        SAML_ASSERTION_AND_MESSAGE_SIGNED                                   | "idp.external.com"        | true          | "known good payload"
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE_AND_ASSERTION                     | "legacy.idp.external.com" | true          | "known good payload with legacy issuer"
        SAML_ASSERTION_AND_MESSAGE_SIGNED                                   | "not.the.idp.com"         | false         | "known good payload"
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE_AND_ASSERTION                     | "not.the.idp.com"         | false         | "known good payload with legacy issuer"
        SAML_ASSERTION_AND_MESSAGE_SIGNED.replace("    ", "")               | "idp.external.com"        | false         | "altered known good payload"
        SAML_LEGACY_ISSUER_SIGNED_MESSAGE_AND_ASSERTION.replace("    ", "") | "legacy.idp.external.com" | false         | "altered known good payload with legacy issuer"
    }

    @Unroll
    def "SAML Utility validator will reject a SAML response with an invalid signature #testDescription"() {
        when: "we unmarshall the SAML string and try to validate the Assertion's signature"
        Response response = samlUtilities.unmarshallResponse(saml)
        def isValidSignature = samlUtilities.validateSignature(response.assertions[0].signature)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the signature was not valid"
        !isValidSignature

        and: "the Issuer was set correctly"
        response.issuer.value == "http://${certificateEntityId}" as String

        where:
        saml                                                    | certificateEntityId       | testDescription
        SAML_ONE_ASSERTION_SIGNED.replace("    ", "")           | "idp.external.com"        | "by tampering with the whitespace of a valid payload"
        SAML_LEGACY_ISSUER_SIGNED_ASSERTION.replace("    ", "") | "legacy.idp.external.com" | "by tampering with the whitespace of a valid payload for a legacy issuer"
        SAML_ASSERTION_INVALID_SIGNATURE                        | "idp.external.com"        | "using a known bad payload"
        samlResponse(issuer() >> assertion(fakeSign: true))     | "idp.external.com"        | "generating a saml:response with an invalid signature in the Assertion"
    }

    @Unroll
    def "SAML Utility can unmarshall a SAML response created using MarkupBuilder with utility methods: #creationMethod"() {
        when: "we unmarshall the SAML string"
        Response response = samlUtilities.unmarshallResponse(saml)

        then: "no exceptions were thrown (SAML was successfully unmarshalled)"
        notThrown(Exception)

        and: "the Issuer was set correctly"
        response.issuer.value == expectedIssuer

        where:
        saml                                                 | expectedIssuer       | creationMethod
        samlResponse(issuer() >> status() >> assertion())    | SAML_EXTERNAL_ISSUER | "issuer(), status(), assertion()"
        samlResponse(issuer() >> status() >> assertion([:])) | SAML_EXTERNAL_ISSUER | "issuer(), status(), assertion([:])"
        samlResponse(issuer(SAML_LEGACY_ISSUER) >> status()) | SAML_LEGACY_ISSUER   | "issuer($SAML_LEGACY_ISSUER), status()"
        samlResponse(issuer() >> assertion(fakeSign: true))  | SAML_EXTERNAL_ISSUER | "issuer(), assertion(fakeSign: true)"
    }
}
